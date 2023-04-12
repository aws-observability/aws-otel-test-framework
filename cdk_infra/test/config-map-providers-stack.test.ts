import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { ConfigMapProvidersStack } from '../lib/stacks/config-map-providers/config-map-providers-stack';

test('Config map providers', () => {
  // prepare
  const stack = new cdk.App();
  const env = {
    region: 'us-west-2',
    account: '123456789012'
  };

  // Act
  const configMapProvidersStack = new ConfigMapProvidersStack(
    stack,
    'config-map-providers',
    {
      env: env,
      bucketNamePrefix: 'adot-collector-integ-test-configurations'
    }
  );

  // Assert
  const template = Template.fromStack(configMapProvidersStack);

  template.hasResourceProperties('AWS::S3::Bucket', {
    BucketName:
      'adot-collector-integ-test-configurations-us-west-2-123456789012'
  });
});
