import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { ConfigMapProvidersStack } from '../lib/stacks/config-map-providers/config-map-providers-stack';

test('Config map providers', () => {
  // prepare
  const stack = new cdk.App();
  const env = {
    region: 'us-west-2'
  };

  // Act
  const configMapProvidersStack = new ConfigMapProvidersStack(
    stack,
    'config-map-providers', {
      suffix: 'foo',
      env: env
  });

  const noSuffixConfigMapProvidersStack = new ConfigMapProvidersStack(stack,
    'no-suffix', {
    env: env
  });

  // Assert
  const template = Template.fromStack(configMapProvidersStack);

  template.hasResourceProperties('AWS::S3::Bucket', {
    BucketName: 'adot-collector-integ-test-configurationsfoo'
  });

  const noSuffixTemplate = Template.fromStack(noSuffixConfigMapProvidersStack);

  noSuffixTemplate.hasResourceProperties('AWS::S3::Bucket', {
    BucketName: 'adot-collector-integ-test-configurations'
  }); 
});
