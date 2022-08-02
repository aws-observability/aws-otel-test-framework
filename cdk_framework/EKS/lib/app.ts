#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { ClusterStack } from './stacks/cluster-stack';
import { deployClusters } from './cluster-deployment';

const app = new cdk.App();

let clusterMap = new Map<string, ClusterStack>()

clusterMap = deployClusters(app);


