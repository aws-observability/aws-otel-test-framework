import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { VPCStack } from '../lib/stacks/vpc-stack';

// example test. To run these tests, uncomment this file along with the
// example resource in lib/my-project-stack.ts
test('ClusterManagementTest', () => {
    const stack = new cdk.App();

    const vpcStack = new VPCStack(stack, "EKSVpc", {
        env: {
          region: 'us-west-2'
        }
      })

        // THEN
    const template = Template.fromStack(vpcStack);

    template.hasResourceProperties("AWS::EC2::VPC", {
        CidrBlock: "10.0.0.0/16"
    })
});
