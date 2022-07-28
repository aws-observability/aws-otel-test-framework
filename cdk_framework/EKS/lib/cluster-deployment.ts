#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { validateClustersConfig } from './utils/validate';
import { VPCStack } from './utils/vpc-stack';
import { aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { ClusterStack } from './stacks/cluster-stack';
import { readFileSync} from 'fs';
const yaml = require('js-yaml')


export function deployClusters(app: cdk.App, configData: any) : Map<string, ClusterStack> {
    const REGION = process.env.REGION || 'us-west-2'

    

    const eksClusterMap = new Map<string, ClusterStack>();
    
    const vpcStack = new VPCStack(app, "EKSVpc", {
      env: {
        region: REGION
      }
    })

    validateClustersConfig(configData)
    for(const [key, value] of Object.entries(configData['clusters'])){
      const val = Object(value)
      const versionKubernetes = eks.KubernetesVersion.of(String(val['version']));
      const newStack = new ClusterStack(app, key, {
        launchType: (val['launch_type']),
        name: key,
        vpc: vpcStack.vpc,
        version: versionKubernetes,
        env: {
          region: REGION
        },
      })
        
    
      eksClusterMap.set(key, newStack)
    }

    return eksClusterMap
}
