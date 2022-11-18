import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2 } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { Vpc } from 'aws-cdk-lib/aws-ec2';
import { KubernetesVersion, Nodegroup } from 'aws-cdk-lib/aws-eks';
import { ManagedPolicy } from 'aws-cdk-lib/aws-iam';
import { GetLayer } from '../../utils/eks/kubectlLayer';

export class EC2Stack extends Stack {
  cluster: eks.Cluster;

  constructor(scope: Construct, id: string, props: EC2ClusterStackProps) {
    super(scope, id, props);

    const logging = [
      eks.ClusterLoggingTypes.API,
      eks.ClusterLoggingTypes.AUDIT,
      eks.ClusterLoggingTypes.AUTHENTICATOR,
      eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
      eks.ClusterLoggingTypes.SCHEDULER
    ];
    const instance_type = props.instance_type.toLowerCase();
    this.cluster = new eks.Cluster(this, props.name, {
      clusterName: props.name,
      vpc: props.vpc,
      vpcSubnets: [{ subnetType: ec2.SubnetType.PUBLIC }],
      defaultCapacity: 0, // we want to manage capacity ourselves
      version: props.version,
      clusterLogging: logging,
      kubectlLayer: GetLayer(this, props.version)
    });
    const clusterNodeGroup = new Nodegroup(this, `${props.name}-managed-ng`, {
      instanceTypes: [new ec2.InstanceType(instance_type)],
      cluster: this.cluster,
      minSize: 2,
      subnets: { subnetType: ec2.SubnetType.PUBLIC }
    });
    clusterNodeGroup.role.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore')
    );
  }
}

export interface EC2ClusterStackProps extends StackProps {
  name: string;
  vpc: Vpc;
  version: KubernetesVersion;
  instance_type: string;
}
