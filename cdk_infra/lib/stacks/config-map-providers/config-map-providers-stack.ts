import { Duration, Stack, StackProps } from 'aws-cdk-lib';
import {
  Effect,
  PolicyStatement,
  AccountRootPrincipal
} from 'aws-cdk-lib/aws-iam';
import { Bucket, BucketEncryption } from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';

export interface ConfigMapProvidersStackProps extends StackProps {
  // Suffix used to test the stack
  suffix?: string;
}

/**
 * Stack containing resources used by config map providers in the integration
 * tests.
 */
export class ConfigMapProvidersStack extends Stack {
  constructor(
    scope: Construct,
    id: string,
    props: ConfigMapProvidersStackProps
  ) {
    super(scope, id, props);

    // We need a suffix for testing deployments in local accounts and in unit tests
    // We are "hiding" this here so that this detail does not leak when we instantiate
    // the stack. The order of precedence when evaluating the suffix is:
    // * environment CONFIG_MAP_PROVIDERS_BUCKET_SUFFIX variable: used when testing locally in dev account.
    // * props.suffix: used in unit tests
    // * the default is an empty suffix ''
    const suffix = process.env.CONFIG_MAP_PROVIDERS_BUCKET_SUFFIX || props.suffix || '';

    const bucketName = `adot-collector-integ-test-configurations${suffix}`;

    const bucket = new Bucket(this, 'configuration-bucket', {
      bucketName: bucketName,
      // Auto delete objects in 3 days
      lifecycleRules: [{ expiration: Duration.days(3) }],
      encryption: BucketEncryption.S3_MANAGED,
      versioned: false
    });

    const bucketPolicy = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: ['s3:*'],
      resources: [`arn:aws:s3:::${bucketName}`, `arn:aws:s3:::${bucketName}/*`],
      principals: [new AccountRootPrincipal()]
    });

    bucket.addToResourcePolicy(bucketPolicy);
  }
}
