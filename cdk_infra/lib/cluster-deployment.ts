#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { VPCStack } from './stacks/vpc/vpc-stack';
import { aws_eks as eks, StackProps } from 'aws-cdk-lib';
import { readFileSync } from 'fs';
import { EC2Stack } from './stacks/eks/ec2-cluster-stack';
import { FargateStack } from './stacks/eks/fargate-cluster-stack';
import { validateFileSchema } from './utils/eks/validate-config-schema';
import { ClusterInterface } from './interfaces/eks/cluster-interface';
import { ec2ClusterInterface } from './interfaces/eks/ec2cluster-interface';
import { validateInterface } from './utils/eks/validate-interface-schema';
import { ClusterAuth } from './constructs/eks/clusterAuthConstruct';
import { HelmChart } from 'aws-cdk-lib/aws-eks';
import { OpenIdConnectProvider } from 'aws-cdk-lib/aws-eks';
import { Vpc } from 'aws-cdk-lib/aws-ec2';

const yaml = require('js-yaml');

export function deployClusters(
  app: cdk.App,
  vpc: Vpc,
  envInput?: StackProps['env']
): Map<string, FargateStack | EC2Stack> {
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
        vpc: vpc,
        version: versionKubernetes,
        instance_type: ec2Cluster.instance_type,
        env: envInput
      });
    } else {
      validateInterface(clusterInterface);
      clusterStack = new FargateStack(
        app,
        `${clusterInterface.name}EKSCluster`,
        {
          name: clusterInterface.name,
          vpc: vpc,
          version: versionKubernetes,
          env: envInput
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
        release: 'cert-manager',
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
    new OpenIdConnectProvider(
      clusterStack,
      `${clusterInterface.name}-oidc-provider`,
      {
        url: clusterStack.cluster.clusterOpenIdConnectIssuerUrl
      }
    );
    eksClusterMap.set(cluster['name'], clusterStack);
  }
  return eksClusterMap;
}
