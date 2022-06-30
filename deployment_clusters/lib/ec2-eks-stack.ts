import { NestedStack, NestedStackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Vpc } from 'aws-cdk-lib/aws-ec2';
import { Version } from 'aws-cdk-lib/aws-lambda';
import { KubernetesVersion, Nodegroup } from 'aws-cdk-lib/aws-eks';
import { CpuArchitecture } from 'aws-cdk-lib/aws-ecs';


export class EC2Stack extends NestedStack {
  constructor(scope: Construct, id: string, props: Arm64Props) {
    super(scope, id, props);

    const armCluster = new eks.Cluster(this, props.name+'-Cluster', {
      clusterName: props.name,
      vpc: props.vpc,
      defaultCapacity: 0,  // we want to manage capacity our selves
      version: props.version
    });

    if(props.cpu == "arm64"){
        armCluster.addNodegroupCapacity('ng-arm', {
            instanceTypes: [new ec2.InstanceType('m6g.large')],
            minSize: 2
        })
    } else {
        armCluster.addNodegroupCapacity('ng-arm', {
            instanceTypes: [new ec2.InstanceType('m5.large')],
            minSize: 2
        })
    }
    

  }
}

export interface Arm64Props extends NestedStackProps{
    name: string;
    vpc: Vpc;
    version: KubernetesVersion;
    cpu: string
}
