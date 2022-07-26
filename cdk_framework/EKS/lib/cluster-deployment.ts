#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { validateClustersConfig } from './utils/validate';
import { VPCStack } from './utils/vpc-stack';
import { aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { ClusterStack } from './stacks/cluster-stack';
import { readFileSync} from 'fs';
const yaml = require('js-yaml')


export function deployClusters(app: cdk.App) : Map<string, eks.ICluster> {
    const REGION = process.env.REGION || 'us-west-2'

    const route = process.env.CDK_CONFIG_PATH ||  __dirname + '/config/clusters.yml';
    const raw = readFileSync(route)
    const configData = yaml.load(raw)

    const eksClusterMap = new Map<string, eks.ICluster>();
    
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
        launchType: String(val['launch_type']),
        name: key,
        vpc: vpcStack.vpc,
        version: versionKubernetes,
        cpu: String(val["cpu_architecture"]),
        nodeSize: String(val["node_size"]),
        env: {
          region: REGION
        },
      })
        
    
      eksClusterMap.set(key, newStack.cluster)
    }

    return eksClusterMap
}