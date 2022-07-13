import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam'
import { Construct } from 'constructs';
// import {FargateNested} from '../fargate-stack';
import {ClusterStack} from '../stacks/nested_clusters/cluster-stack'
import {validateClusters} from '../utils/parse' 
import { readFileSync, writeFileSync } from 'fs';
import {ManagedPolicy} from 'aws-cdk-lib/aws-iam';
const yaml = require('js-yaml')



export class ClusterManagementStack extends Stack {
  clusterMap = new Map();

  constructor(scope: Construct, id: string, props: ParentStackProps) {
    super(scope, id, props);

     // IAM role for our EC2 worker nodes
     const workerRole = new iam.Role(this, 'EKSWorkerRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [
        ManagedPolicy.fromAwsManagedPolicyName("AmazonPrometheusRemoteWriteAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("AWSXrayWriteOnlyAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("CloudWatchAgentAdminPolicy"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonS3ReadOnlyAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("AWSAppMeshEnvoyAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonEKSWorkerNodePolicy"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryReadOnly"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonEKS_CNI_Policy")
      ]
    });


    
    const vpc = new ec2.Vpc(this, 'EKSVpc',
     {cidr: "10.0.0.0/16",
     natGateways: 1,
     vpnGateway: true,
     availabilityZones: ['us-west-2a', 'us-west-2b', 'us-west-2c'],
     subnetConfiguration: [
      {
        cidrMask: 24,
        subnetType: ec2.SubnetType.PRIVATE_WITH_NAT,
        name: 'private_subnet'
      },
      {
        cidrMask: 24,
        subnetType: ec2.SubnetType.PUBLIC,
        name: "public_subnet"
      }
     ] 
    });  
    const data = props.data
    validateClusters(data)
    for(const [clusterName, rawFields] of Object.entries(data['clusters'])){
      const fields = Object(rawFields)
      const versionKubernetes = eks.KubernetesVersion.of(String(fields['version']));
      const newStack = new ClusterStack(this, clusterName + "Stack", {
        launch_type: String(fields['launch_type']),
        name: clusterName,
        vpc: vpc,
        version: versionKubernetes,
        cpu: String(fields["cpu_architecture"]),
        node_size: String(fields["node_size"])
        
      })
      this.clusterMap.set(clusterName, newStack.cluster)
    }

    


  }

  getCluster(clusterName: string) : eks.Cluster | eks.FargateCluster | null {
    if(!this.clusterMap.has(clusterName)){
      return null
    }
    return this.clusterMap.get(clusterName)
  }

  

  
}


export interface ParentStackProps extends StackProps{
  data: any
}