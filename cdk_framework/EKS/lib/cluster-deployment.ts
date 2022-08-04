#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { validateClustersConfig } from './utils/validate-cluster-config';
import { VPCStack } from './stacks/vpc-stack';
import { aws_eks as eks} from 'aws-cdk-lib';
import { ClusterStack } from './stacks/cluster-stack';
import { readFileSync} from 'fs';
const yaml = require('js-yaml')


export function deployClusters(app: cdk.App) : Map<string, ClusterStack> {
    const REGION = process.env.REGION || 'us-west-2'

    const route = process.env.CDK_CONFIG_PATH ||  __dirname + '/config/cluster-config/clusters.yml';

    if (!/(.yml|.yaml)$/.test(route)){
      throw new Error ('Path for cluster configuration must be to a yaml file')
  }

    const raw = readFileSync(route)
    const configData = yaml.load(raw)

    const eksClusterMap = new Map<string, ClusterStack>();
    
    const vpcStack = new VPCStack(app, 'EKSVpc', {
      env: {
        region: REGION
      }
    })

    validateClustersConfig(configData)
    for(const [key, value] of Object.entries(configData['clusters'])){
      const val = Object(value)
      const versionKubernetes = eks.KubernetesVersion.of(String(val['version']));
      const newStack = new ClusterStack(app, key + 'EKSCluster', {
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
