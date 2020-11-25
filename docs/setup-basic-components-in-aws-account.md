# Setup basic components in aws account

To run any types of test rather than the `mock folder`, the testing framework will need to create resources in your aws account. 
there're some basic components needed before run the tests.

## 1. Setup your aws credentials
Refer to: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html

Please ensure the default profile in your ~/.aws/credentials has Admin permission.

## 2. Setup basic components

Setup only needs to be run once

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
