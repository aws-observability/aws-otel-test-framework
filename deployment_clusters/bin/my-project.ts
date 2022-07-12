#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { ClusterManagementStack } from '../lib/cluster-management-stack';
import {parseData, validateClusters} from '../lib/utils/parse' 
import { fileURLToPath } from 'url';
import { resolve, dirname } from 'path';
import { readFileSync} from 'fs';
const yaml = require('js-yaml')


const route = '../deployment_clusters/bin/clusters.yml';
const raw = readFileSync(route)
const data = yaml.load(raw)
const app = new cdk.App();
new ClusterManagementStack(app, 'DeploymentStack', {
    data: data,
    env: {
        region: 'us-west-2'
    },
});
const route2 = '../deployment_clusters/bin/clusters2.yml';
const raw2 = readFileSync(route2)
const data2 = yaml.load(raw2)
new ClusterManagementStack(app, "DStack2", {
    data: data2,
    env: {
        region: 'us-west-2'
    },
})