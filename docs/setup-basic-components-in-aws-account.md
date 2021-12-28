# Setup basic components in aws account

To run any types of test rather than the `mock folder`, the testing framework will need to create resources in your aws account. 
there're some basic components needed before run the tests.

## 1. Setup your aws credentials
Refer to: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html

Please ensure the default profile in your ~/.aws/credentials has Admin permission.

## 2. Setup basic components
Setup only needs to be run once, it creates:

1. one iam role
2. one vpc
3. one security group
4. two ecr repos, one for sample apps, one for mocked server
5. one s3 bucket, one dynamodb table

```shell
cd aws-otel-test-framework/terraform/setup 
terraform init
terraform apply
```

###2.1 Upload setup's terraform state to s3 bucket
Prerequisite: 
- you are required to run the [setup basic components](setup-basic-components-in-aws-account.md#2-setup-basic-components) once if you did not setup those components before.
- Uncomment the [backend configuration](https://github.com/khanhntd/aws-otel-test-framework/blob/support_s3_bucket_setup/terraform/setup/backend.tf#L17-L25)

```shell
cd aws-otel-test-framework/terraform/setup 
terraform init
terraform apply
```
## 3. Build sample app images

this step might take 20 minutes, it builds and pushes the sample apps and mocked server images to the ecr repos, so that the following test could use them.

```shell
cd aws-otel-test-framework/terraform/imagebuild
terraform init
terraform apply
```

Remember, if you have changes on sample apps or the mocked server, you need to rerun this imagebuild task.
