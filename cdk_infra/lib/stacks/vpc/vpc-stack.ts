import { Stack, StackProps, aws_ec2 as ec2 } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class VPCStack extends Stack {
  vpc: ec2.Vpc;

  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);
    this.vpc = new ec2.Vpc(this, 'EKSVpc', {
      cidr: '10.0.0.0/16',
      natGateways: 1,
      vpnGateway: true,
      //https://github.com/aws/aws-cdk/issues/21690
      availabilityZones: Stack.of(this).availabilityZones.sort().slice(0, 1),
      subnetConfiguration: [
        {
          cidrMask: 24,
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
          name: 'private_subnet'
        },
        {
          cidrMask: 24,
          subnetType: ec2.SubnetType.PUBLIC,
          name: 'public_subnet'
        }
      ]
    });
  }
}
