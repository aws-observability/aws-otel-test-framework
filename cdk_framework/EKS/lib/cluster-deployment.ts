#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { VPCStack } from './stacks/vpc-stack';
import { aws_eks as eks } from 'aws-cdk-lib';
import { readFileSync } from 'fs';
import { EC2Stack } from './stacks/ec2-cluster-stack';
import { FargateStack } from './stacks/fargate-cluster-stack';
import { validateFileSchema } from './utils/validate-config-schema';
import { ClusterInterface } from './interfaces/cluster-interface';
import { ec2ClusterInterface } from './interfaces/ec2cluster-interface';
import { validateInterface } from './utils/validate-interface-schema';
import { ClusterAuth } from './constructs/clusterAuthConstruct';
import { HelmChart } from 'aws-cdk-lib/aws-eks';

const yaml = require('js-yaml');

export function deployClusters(
  app: cdk.App
): Map<string, FargateStack | EC2Stack> {
  const REGION = process.env.REGION || 'us-west-2';

  const route =
    process.env.CDK_CONFIG_PATH ||
    __dirname + '/config/cluster-config/clusters.yml';

  if (!/(.yml|.yaml)$/.test(route)) {
    throw new Error('Path for cluster configuration must be to a yaml file');
  }

  const raw = readFileSync(route);
  const configData = yaml.load(raw);

  validateFileSchema(configData);

  const eksClusterMap = new Map<string, FargateStack | EC2Stack>();

  const vpcStack = new VPCStack(app, 'EKSVpc', {
    env: {
      region: REGION
    }
  });

  const clusterNameSet = new Set();
  for (const cluster of configData['clusters']) {
    let clusterStack;
    const clusterInterface = cluster as ClusterInterface;
    if (clusterNameSet.has(clusterInterface.name)) {
      throw new Error(
        `Cluster name ${clusterInterface.name} is shared by two different clusters`
      );
    }
    clusterNameSet.add(clusterInterface.name);
    const versionKubernetes = eks.KubernetesVersion.of(
      clusterInterface.version
    );

    if (clusterInterface.launch_type === 'ec2') {
      const ec2Cluster = cluster as ec2ClusterInterface;
      validateInterface(ec2Cluster);
      clusterStack = new EC2Stack(app, `${ec2Cluster.name}EKSCluster`, {
        name: ec2Cluster.name,
        vpc: vpcStack.vpc,
        version: versionKubernetes,
        instance_type: ec2Cluster.instance_type,
        env: {
          region: REGION
        }
      });
    } else {
      validateInterface(clusterInterface);
      clusterStack = new FargateStack(
        app,
        `${clusterInterface.name}EKSCluster`,
        {
          name: clusterInterface.name,
          vpc: vpcStack.vpc,
          version: versionKubernetes,
          env: {
            region: REGION
          }
        }
      );
    }
    new ClusterAuth(clusterStack, `${clusterInterface.name}ClusterAuth`, {
      cluster: clusterStack.cluster
    });

    if (clusterInterface.cert_manager) {
      const certManagerHelm = new HelmChart(clusterStack, 'cert-manager', {
        cluster: clusterStack.cluster,
        createNamespace: true,
        namespace: 'cert-manager',
        repository: 'https://charts.jetstack.io',
        version: 'v1.10.0',
        chart: 'cert-manager',
        /**
         * Default release name relies on the cfn node id but
         * the node id for the stack can contain an incompatible format.
         * Force default release name instead.
         */
        release: 'cert-manager-2',
        // values should be passed as objects
        // https://github.com/aws/aws-cdk/issues/11475#issuecomment-855220507
        values: {
          installCRDs: 'true',
          // https://github.com/cert-manager/cert-manager/issues/3237#issuecomment-827523656
          webhook: {
            securePort: 10260
          }
        }
      });
      certManagerHelm.node.addDependency(clusterStack.cluster);
    }
    eksClusterMap.set(cluster['name'], clusterStack);
  }

  return eksClusterMap;
}
