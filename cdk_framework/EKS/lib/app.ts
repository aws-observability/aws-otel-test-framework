#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { readFileSync} from 'fs';
import { aws_eks as eks} from 'aws-cdk-lib';
import { ClusterStack } from './stacks/cluster-stack';
import { deployClusters } from './cluster-deployment';
const yaml = require('js-yaml')
// import yaml from 'js-yaml'

const app = new cdk.App();

const route = process.env.CDK_CONFIG_PATH ||  __dirname + '/config/clusters.yml';
const raw = readFileSync(route)
const configData = yaml.load(raw)

let clusterMap = new Map<string, ClusterStack>()

clusterMap = deployClusters(app, configData);

function getCluster(clusterName: string) : eks.ICluster | null {
    const clusterStack = clusterMap.get(clusterName)
    return clusterStack == undefined ? null : clusterStack.cluster
}
