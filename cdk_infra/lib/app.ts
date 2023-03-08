#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { deployClusters } from './cluster-deployment';
import { VPCStack } from './stacks/vpc/vpc-stack';
import { MSKClustersStack } from './stacks/msk/msk-stack';
import { ConfigMapProvidersStack } from './stacks/config-map-providers/config-map-providers-stack';

const envDefault = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION
};
const app = new cdk.App();

const vpcStack = new VPCStack(app, 'EKSVpc', {
  env: envDefault
});

new MSKClustersStack(app, 'msk-clusters', {
  env: envDefault,
  eksVPC: vpcStack.vpc
});
new ConfigMapProvidersStack(app, 'config-map-providers', {
  suffix: process.env.CONFIG_BUCKET_SUFFIX
});
deployClusters(app, vpcStack.vpc, envDefault);
