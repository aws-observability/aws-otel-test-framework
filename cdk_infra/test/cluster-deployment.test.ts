import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { readFileSync } from 'fs';
import { deployClusters } from '../lib/cluster-deployment';
import { ClusterInterface } from '../lib/interfaces/eks/cluster-interface';
import { EC2Stack } from '../lib/stacks/eks/ec2-cluster-stack';
import { FargateStack } from '../lib/stacks/eks/fargate-cluster-stack';
import { VPCStack } from '../lib/stacks/vpc/vpc-stack';

const yaml = require('js-yaml');

test('ClusterTest', () => {
  const app = new cdk.App();

  process.env.CDK_CONFIG_PATH = __dirname + '/test_config/test_clusters.yml';

  const route = __dirname + '/test_config/test_clusters.yml';
  const raw = readFileSync(route);
  const data = yaml.load(raw);

  let clusterMap = new Map<string, FargateStack | EC2Stack>();
  const versionMap = new Map<string, string>();
  const vpc = new VPCStack(app, 'testVPC');
  clusterMap = deployClusters(app, vpc.vpc);
  for (const cluster of data['clusters']) {
    const clusterInterface = cluster as ClusterInterface;
    versionMap.set(clusterInterface.name, clusterInterface.version);
  }

  for (const [key, st] of clusterMap) {
    const template = Template.fromStack(st);
    const KubernetesVersion = versionMap.get(key);

    template.hasResourceProperties('Custom::AWSCDK-EKS-Cluster', {
      Config: {
        name: key,
        version: KubernetesVersion,
        logging: {
          clusterLogging: [
            {
              enabled: true,
              types: [
                'api',
                'audit',
                'authenticator',
                'controllerManager',
                'scheduler'
              ]
            }
          ]
        }
      }
    });
    template.resourceCountIs('Custom::AWSCDK-EKS-KubernetesResource', 2);
    template.resourceCountIs('Custom::AWSCDKOpenIdConnectProvider', 1);
  }
});
