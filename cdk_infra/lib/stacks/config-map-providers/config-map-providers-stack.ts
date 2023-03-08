import { Duration, Stack, StackProps } from 'aws-cdk-lib';
import {
  Effect,
  PolicyStatement,
  AccountRootPrincipal
} from 'aws-cdk-lib/aws-iam';
import { Bucket, BucketEncryption } from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';

export interface ConfigMapProvidersStackProps extends StackProps {
  // Suffix used to test the stack and avoid collisions with s3 buckets names, that must be globally unique.
  suffix: string | undefined;
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

    const suffix = props.suffix || '';
    const bucketName = `aws-otel-collector-integ-test-configurations${suffix}`;

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
