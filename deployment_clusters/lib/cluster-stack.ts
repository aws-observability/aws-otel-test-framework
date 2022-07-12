import { NestedStack, NestedStackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Vpc } from 'aws-cdk-lib/aws-ec2';
import { Version } from 'aws-cdk-lib/aws-lambda';
import { KubernetesVersion, Nodegroup } from 'aws-cdk-lib/aws-eks';
import { CpuArchitecture } from 'aws-cdk-lib/aws-ecs';


export class ClusterStack extends NestedStack {
  cluster : eks.Cluster | eks.FargateCluster

  constructor(scope: Construct, id: string, props: ClusterStackProps) {
    super(scope, id, props);

    if(props.launch_type === 'ec2'){
      this.cluster = new eks.Cluster(this, props.name+'-Cluster', {
      clusterName: props.name,
      vpc: props.vpc,
      defaultCapacity: 0,  // we want to manage capacity our selves
      version: props.version,
      clusterLogging: [
        eks.ClusterLoggingTypes.API,
        eks.ClusterLoggingTypes.AUDIT,
        eks.ClusterLoggingTypes.AUTHENTICATOR,
        eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
        eks.ClusterLoggingTypes.SCHEDULER,
      ]
    });
      if(props.cpu === "arm_64"){
          this.cluster.addNodegroupCapacity('ng-arm', {
              instanceTypes: [new ec2.InstanceType('m6g.' + props.node_size)],
              minSize: 2
          })
      } else {
          this.cluster.addNodegroupCapacity('ng-arm', {
              instanceTypes: [new ec2.InstanceType('m5.' + props.node_size)],
              minSize: 2
          })
      }
    }

    if(props.launch_type === 'fargate'){
      console.log("Fargate starting")
      this.cluster = new eks.FargateCluster(this, props.name + '-Cluster', {
        clusterName: props.name,
        vpc: props.vpc,
        // defaultCapacity: 0,  // we want to manage capacity our selves
        version: props.version,
        clusterLogging: [
          eks.ClusterLoggingTypes.API,
          eks.ClusterLoggingTypes.AUDIT,
          eks.ClusterLoggingTypes.AUTHENTICATOR,
          eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
          eks.ClusterLoggingTypes.SCHEDULER,
        ]
      });
    }


  }
}

export interface ClusterStackProps extends NestedStackProps{
    launch_type: string;
    name: string;
    vpc: Vpc;
    version: KubernetesVersion;
    cpu: string
    node_size: string
}
