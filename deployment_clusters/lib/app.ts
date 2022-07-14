#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { ClusterManagementStack } from './stacks/cluster-management-stack';
import { readFileSync} from 'fs';
import { ClusterResourceStack } from './stacks/clusterResourcestack';
import { validateClusters } from './utils/parse';
import { VPCStack } from './utils/vpc-stack';
import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { ClusterStack } from './stacks/cluster-stack';
const yaml = require('js-yaml')







const app = new cdk.App();

const clusterMap = new Map<string, eks.ICluster>();

const vs = new VPCStack(app, "EKSVpc", {
  env: {
    region: 'us-west-2'
  }
})


// console.log(process.env.CDK_CONFIG_PATH)
const route = process.env.CDK_CONFIG_PATH ||  __dirname + '/config/clusters.yml';
// const route =  __dirname + '/config/clusters.yml';
const raw = readFileSync(route)
const data = yaml.load(raw)



validateClusters(data)
// const bigMap = parseData(data['clusters'])
for(const [key, value] of Object.entries(data['clusters'])){
  const val = Object(value)
  const versionKubernetes = eks.KubernetesVersion.of(String(val['version']));
  const newStack = new ClusterStack(app, key + "Stack", {
    launch_type: String(val['launch_type']),
    name: key,
    vpc: vs.vpc,
    version: versionKubernetes,
    cpu: String(val["cpu_architecture"]),
    node_size: String(val["node_size"]),
    env: {
      region: 'us-west-2'
    },
  })
  clusterMap.set(key, newStack.cluster)
}


function getCluster(clusterName: string) : eks.ICluster | undefined {
  return clusterMap.get(clusterName)
}





// const route = __dirname + '/config/clusters.yml';
// const raw = readFileSync(route)
// const data = yaml.load(raw)
// const app = new cdk.App();


// const cms = new ClusterManagementStack(app, 'ClusterManagementStack', {
//     data: data,
//     env: {
//         region: 'us-west-2'
//     },
// });

// if(process.env.CDK_EKS_RESOURCE_DEPLOY){

//     const crs = new ClusterResourceStack(app,"eks-cluster-resource",{
//         clusterManagementStack:cms
//     })
//     crs.addDependency(cms)
// }