import { Stack, StackProps, aws_ec2 as ec2 } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class VPCStack extends Stack {
  vpc: ec2.Vpc;

  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);
    this.vpc = new ec2.Vpc(this, 'EKSVpc', {
      cidr: '10.0.0.0/16',
      natGateways: 3,
      maxAzs: 3,
      vpnGateway: false,
      subnetConfiguration: [
        {
          cidrMask: 19,
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
          name: 'private_subnet'
        },
        {
          cidrMask: 19,
          subnetType: ec2.SubnetType.PUBLIC,
          name: 'public_subnet'
        }
      ]
    });
  }
}
