import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam'
import { Construct } from 'constructs';
import {FargateNested} from '../lib/fargate-stack';
import { EC2Stack } from './ec2-eks-stack';
import {parseData, validateClusters} from '../lib/utils/parse' 
import { readFileSync, writeFileSync } from 'fs';
const yaml = require('js-yaml')



export class MyProjectStack extends Stack {
  constructor(scope: Construct, id: string, props: ParentStackProps) {
    super(scope, id, props);

     // IAM role for our EC2 worker nodes
     const workerRole = new iam.Role(this, 'EKSWorkerRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com')
    });


    
    const vpc = new ec2.Vpc(this, 'EKSVpc');  // Create a new VPC for our cluster

    // const raw = readFileSync(props.route)
    // const data = yaml.load(raw)
    const data = props.data
    // if(!data['clusters']){
    //   throw new Error('No clusters field being filed in the yaml file')
    // }
    // // if(Object.keys(data['clusers']).length == 0){
    // //   throw new Error('No clusters being defined in the yaml file')
    // // }
    validateClusters(data['clusters'])
    const bigMap = parseData(data['clusters'])
    for(const [key, value] of Object.entries(data['clusters'])){
      const val = Object(value)
      const versionKubernetes = eks.KubernetesVersion.of(String(val['version']));
      if(String(val['launch_type']) === 'ec2'){
        const arm64Cluster = new EC2Stack(this, key + "Stack", {
          name: key,
          vpc: vpc,
          version: versionKubernetes,
          cpu: String(val["cpu_architecture"])
        });
      } else if(String(val['launch_type']) === 'fargate'){
        const fargateCluster = new FargateNested(this, key + "Stack", {
          name: key,
          vpc: vpc,
          version: versionKubernetes
        });
      } else {
        console.log('-------------------------------------------')
        console.log('nothing deployed: error with configuration')
        console.log('------------------------------------------------')
      }
    }

    

    

    // const versionKubernetes = eks.KubernetesVersion.of('1.22');
    
   
    // const versionKubernetes = eks.KubernetesVersion.of(String(data['clusters']['armCluster']['version']));

    // const armCluster = new eks.Cluster(this, String(data['clusters']['armCluster']['version']) + "-Stack", {
    //   clusterName: String(data['clusters']['armCluster']['version']) + "-Cluster",
    //   vpc: vpc,
    //   // defaultCapacity: 0,  // we want to manage capacity our selves
    //   version: versionKubernetes
    // });
    


  }

  

  
}


export interface ParentStackProps extends StackProps{
  data: any
}