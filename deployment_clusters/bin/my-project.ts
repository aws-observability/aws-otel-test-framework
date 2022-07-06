#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { MyProjectStack } from '../lib/my-project-stack';
import {parseData, validateClusters} from '../lib/utils/parse' 
import { fileURLToPath } from 'url';
import { resolve, dirname } from 'path';
import { readFileSync} from 'fs';
const yaml = require('js-yaml')


// try {
//   const raw = readFileSync('../my-project/bin/clusters.yml')
//   const data = yaml.load(raw)
//   console.log(data)
//   console.log("-----------")
//   validateClusters(data['clusters'])
//   if(!data['clusters']['amdCluster']['target']){
//     console.log(true)
//   } else {
//     console.log(false)
//   }
//   data['clusters']['armCluster']['target'] = 11
//   data['clusters']['fargateCluster']['target'] = 12
//   console.log(data)
//   console.log(typeof data['clusters']['amdCluster']['version'])
//   console.log(typeof data['clusters']['amdCluster']['cpu_architecture'])
//   console.log(typeof data['clusters']['amdCluster']['launch_type'])
// } catch (e) {
//   console.log(e);
// }


const route = '../deployment_clusters/bin/clusters.yml';
const raw = readFileSync(route)
const data = yaml.load(raw)
const app = new cdk.App();
new MyProjectStack(app, 'DeploymentStack', {data: data});