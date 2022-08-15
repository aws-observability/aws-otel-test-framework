import { Stack, StackProps, aws_ec2 as ec2} from 'aws-cdk-lib';
import { Construct } from 'constructs';



export class VPCStack extends Stack {
  vpc: ec2.Vpc;

  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    const REGION = process.env.REGION || 'us-west-2'


    this.vpc = new ec2.Vpc(this, 'EKSVpc',
     {cidr: '10.0.0.0/16',
     natGateways: 1,
     vpnGateway: true,
     availabilityZones: [REGION + 'a', REGION + 'b', REGION + 'c'],
     subnetConfiguration: [
      {
        cidrMask: 24,
        subnetType: ec2.SubnetType.PRIVATE_WITH_NAT,
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
