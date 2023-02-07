import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { VPCStack } from '../lib/stacks/vpc/vpc-stack';
import { MSKClustersStack } from '../lib/stacks/msk/msk-stack';
import { Stack, StackProps } from 'aws-cdk-lib';
import { IVpc } from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

class FakeStack extends Stack {
  readonly vpc: IVpc;
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);
    this.vpc = new cdk.aws_ec2.Vpc(this, 'vpc', { vpcName: 'aoc-vpc' });
  }
}
// example test. To run these tests, uncomment this file along with the
// example resource in lib/my-project-stack.ts
test('ClusterManagementTest', () => {
  // prepare
  const stack = new cdk.App();
  const env = {
    region: 'us-west-2'
  };

  // Will simulate the vpc created in terraform
  const fakeStack = new FakeStack(stack, 'fake-stack', { env: env });

  const vpcStack = new VPCStack(stack, 'EKSVpc', {
    env: env
  });

  // act
  const mskClustersStack = new MSKClustersStack(stack, 'MskClusters', {
    env: env,
    eksVPC: vpcStack.vpc,
    aocVPC: fakeStack.vpc
  });

  //asert
  const template = Template.fromStack(mskClustersStack);

  template.hasResourceProperties('AWS::MSK::Cluster', {
    ClusterName: 'AOCMSKCluster2-8-1'
  });
  template.hasResourceProperties('AWS::MSK::Cluster', {
    ClusterName: 'AOCMSKCluster3-2-0'
  });
  template.hasResourceProperties('AWS::MSK::Cluster', {
    ClusterName: 'EKSMSKCluster2-8-1'
  });
  template.hasResourceProperties('AWS::MSK::Cluster', {
    ClusterName: 'EKSMSKCluster3-2-0'
  });
});
