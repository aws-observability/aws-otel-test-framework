import { Duration, Stack, StackProps } from 'aws-cdk-lib';
import {
  Effect,
  PolicyStatement,
  AccountRootPrincipal
} from 'aws-cdk-lib/aws-iam';
import { Bucket, BucketEncryption } from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';

export interface ConfigMapProvidersStackProps extends StackProps {
  /* Prefix of the bucket that will store the configuration for confmap providers */
  bucketNamePrefix: string
}
/**
 * Stack containing resources used by config map providers in the integration
 * tests. The limitation is that only one stack with a specific prefix can be deployed per
 * region and account.
 */
export class ConfigMapProvidersStack extends Stack {
  constructor(scope: Construct, id: string, props: ConfigMapProvidersStackProps) {
    super(scope, id, props);

    // We need a globally unique bucket name that can be used in local development as well,
    // therefore we are using the region and account id as part of the bucket name.
    const bucketName = `${props.bucketNamePrefix}-${this.region}-${this.account}`;

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
