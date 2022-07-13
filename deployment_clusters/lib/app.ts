#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { ClusterManagementStack } from './stacks/cluster-management-stack';
import { readFileSync} from 'fs';
import { ClusterResourceStack } from './stacks/clusterResourcestack';
const yaml = require('js-yaml')


const route = process.env.CDK_CONFIG_PATH || __dirname + '/config/clusters.yml';
const raw = readFileSync(route)
const data = yaml.load(raw)
const app = new cdk.App();


const cms = new ClusterManagementStack(app, 'ClusterManagementStack', {
    data: data,
    env: {
        region: 'us-west-2'
    },
});

if(process.env.CDK_EKS_RESOURCE_DEPLOY){

    const crs = new ClusterResourceStack(app,"eks-cluster-resource",{
        clusterManagementStack:cms
    })
    crs.addDependency(cms)
}