import { NestedStack, NestedStackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Vpc } from 'aws-cdk-lib/aws-ec2';
import { Version } from 'aws-cdk-lib/aws-lambda';
import { KubernetesVersion } from 'aws-cdk-lib/aws-eks';



export class FargateNested extends NestedStack {
  constructor(scope: Construct, id: string, props: FargateProps) {
    super(scope, id, props);

    const eksFargateCluster = new eks.FargateCluster(this, 'Fargate-Cluster', {
      clusterName: props.name,
      vpc: props.vpc,
      // defaultCapacity: 0,  // we want to manage capacity our selves
      version: props.version
    });
  }
}

export interface FargateProps extends NestedStackProps{
    name: string;
    vpc: Vpc;
    version: KubernetesVersion;
}
