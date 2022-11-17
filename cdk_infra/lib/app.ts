#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { deployClusters } from './cluster-deployment';
import { VPCStack } from './stacks/vpc/vpc-stack';
import { StackProps } from 'aws-cdk-lib';

const envDefault = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION
};
const app = new cdk.App();

const vpcStack = new VPCStack(app, 'EKSVpc', {
  env: envDefault
});
deployClusters(app, vpcStack.vpc, envDefault);
