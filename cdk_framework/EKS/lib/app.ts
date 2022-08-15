#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { deployClusters } from './cluster-deployment';
import { FargateStack } from './stacks/fargate-cluster-stack';
import { EC2Stack } from './stacks/ec2-cluster-stack';

const app = new cdk.App();

let clusterMap = new Map<string, FargateStack | EC2Stack>()

clusterMap = deployClusters(app);


