#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { validateClusters } from './utils/parse';
import { VPCStack } from './utils/vpc-stack';
import { aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { ClusterStack } from './stacks/cluster-stack';
const yaml = require('js-yaml')


export function deployClusters(app: cdk.App, data: any) : Map<string, ClusterStack> {
    const REGION = process.env.REGION || 'us-west-2'

    const clusterMap = new Map<string, ClusterStack>();
    
    const vs = new VPCStack(app, "EKSVpc", {
      env: {
        region: REGION
      }
    })

    validateClusters(data)
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
          region: REGION
        },
      })
        
    
      clusterMap.set(key, newStack)
    }

    return clusterMap
}