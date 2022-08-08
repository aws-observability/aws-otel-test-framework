#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { deployClusters } from './cluster-deployment';
import { deployResources } from './resource-deployment';

const app = new cdk.App();

const clusterStackMap = deployClusters(app);

// resource deployment
if (process.env.CDK_EKS_RESOURCE_DEPLOY == 'true') {
    deployResources(app, clusterStackMap)
}