import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { readFileSync, writeFileSync } from 'fs';
import { VPCStack } from '../lib/utils/vpc-stack';
const yaml = require('js-yaml')

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

    

    template.hasResourceProperties("AWS::IAM::Role", {
        AssumeRolePolicyDocument: {
            Statement: [
                {
                  Principal: {
                    Service: "ec2.amazonaws.com"
                  }
                }
              ]
        },
    })

    template.hasResourceProperties("AWS::EC2::VPC", {
        CidrBlock: "10.0.0.0/16"
    })
});
