#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { readFileSync} from 'fs';
import { aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { ClusterStack } from './stacks/cluster-stack';
import { deployClusters } from './cluster-deployment';
const yaml = require('js-yaml')







const app = new cdk.App();

var clusterMap = new Map<string, eks.ICluster>()

const route = process.env.CDK_CONFIG_PATH ||  __dirname + '/config/clusters.yml';
const raw = readFileSync(route)
const configData = yaml.load(raw)

clusterMap = deployClusters(app);


function getCluster(clusterName: string) : eks.ICluster | null {
    var cluster = clusterMap.get(clusterName)
    return cluster == undefined ? null : cluster
}