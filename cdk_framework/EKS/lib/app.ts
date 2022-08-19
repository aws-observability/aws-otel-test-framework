#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { deployClusters } from './cluster-deployment';

const app = new cdk.App();

const clusterMap = deployClusters(app);
