## 1. Run testing framework locally
 
1. Clone this repo:
````
git clone git@github.com:aws-observability/aws-otel-test-framework.git
````
2. Clone the ADOT Collector repo:
````
git clone git@github.com:aws-observability/aws-otel-collector.git
````
3. Install Terraform: https://learn.hashicorp.com/tutorials/terraform/install-cli
 
4. Install Docker compose: https://docs.docker.com/compose/install/
 
5. Run one of the test cases:
````shell script
cd aws-otel-test-framework/terraform/mock
terraform init
terraform apply -var="testcase=../testcases/otlp_mock" 
terraform destroy
````
 
#### What does the test do?
 
1. Builds collector image from the directory ../aws-otel-collector
2. Runs the collector, sample app, and mock server in docker.
3. Validates if the mock server receives data from collector.
 
 
## 2. Run test cases in AWS platform
 
In the case that you want to debug for a certain platform, you can also use this testing framework to run your test case locally in multiple AWS platforms including EC2, ECS, and EKS.
 
### 2.1 Prerequisite

#### 2.1.0
- docker installed locally
- awscli installed locally
 
#### 2.1.1 Setup your aws credentials
Refer to: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html

#### 2.1.2 Setup unique bucket ID

First create a unique S3 bucket identifier that will be appened to your S3 bucket names. This will
ensure that the S3 bucket name is globally unique. The UUID can be generated with any method of your
choosing.
See [here](https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html) for S3 bucket naming rules.
```shell
export TF_VAR_bucketUUID=$(dd if=/dev/urandom bs=1k count=1k | shasum | cut -b 1-8)
```

 
#### 2.1.3 Run Setup
Setup only needs to be run once, it creates:
 
1. one iam role
2. one vpc
3. one security group
4. two ecr repos, one for sample apps, one for mocked server
5. one amazon managed service for prometheus endpoint. 
6. one s3 bucket, one dynamodb table

Run 
````
cd terraform/setup && terraform init && terraform apply -auto-approve
````
 
And Run
````
cd terraform/imagebuild && terraform init && terraform apply -auto-approve
````
this task will build and push the sample apps and mocked server images to the ecr repos,
 so that the following test could use them.
 
Remember, if you have changes on sample apps or the mocked server, you need to rerun this imagebuild task.

#### 2.1.4 Share Setup resources (Optional)
**Prerequisite:**
- you are required to run the [setup basic components](setup-basic-components-in-aws-account.md#2-setup-basic-components) once if you and other developers did not setup these components before.
- Uncomment the [backend configuration](https://github.com/khanhntd/aws-otel-test-framework/blob/support_s3_bucket_setup/terraform/setup/backend.tf#L17-L25) to share the setup's terraform state

**Advantage:**
- Avoid creating duplicate resources on the same account and having duplicate-resources error when running test case such as VPC.
- Sharing up-to-date resource with other developers instead of creating required resources from scratch.

```shell
cd aws-otel-test-framework/terraform/setup 
terraform init
terraform apply
```
#### 2.1.5 Build AES Otel Collector Docker Image
Please [build your image with the new component](https://github.com/aws-observability/aws-otel-collector/blob/main/docs/developers/build-docker.md), push this image to dockerhub, and record the image link, which will be used in your testing.

#### 2.1.6 Documentation
- [Setup basic components in aws account](setup-basic-components-in-aws-account.md)

### 2.2 Run in EC2
````
cd terraform/ec2 && terraform init && terraform apply -auto-approve \
    -var="aoc_image_repo={{the docker image repo name you just pushed}}" \
    -var="aoc_version={{the aoc binary version}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
````

Don't forget to clean up your resources:
````
terraform destroy -auto-approve
````

### 2.3 Run in ECS
 
````
cd terraform/ecs && terraform init && terraform apply -auto-approve \
    -var="aoc_image_repo={{the docker image repo name you just pushed}}" \
    -var="aoc_version={{ the docker image tag name}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
````
 
Don't forget to clean up your resources:
````
terraform destroy -auto-approve
````
 
### 2.4 Run in EKS
**Prerequisite:** you are required to create an EKS cluster in your account
````
cd terraform/eks && terraform init && terraform apply -auto-approve \
    -var="aoc_image_repo={{the docker image you just pushed}}" \
    -var="aoc_version={{ the docker image tag name}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars" \
    -var="eks_cluster_name={{the eks cluster name in your account}}" 
````

 Don't forget to clean up your resources:
````
terraform destroy -auto-approve \
    -var="eks_cluster_name={the eks cluster name in your account}"
````

### 2.4.1 Run in EKS Fargate
#### Create a new fargate cluster (optional in integ test account required in person account)

```
cd terraform/eks_fargate_setup && terraform apply -auto-approve -var="eks_cluster_name=<your_cluster>"
```

#### How to run fargate tests
Add -var="deployment_type=fargate" to the eks creation statement
Supported tests
* otlp_mock

Not supported tests
* otlp_trace
  * This is because no sts role given to the sample app. 

Test
```
cd terraform/eks && terraform apply -auto-approve \
  -var="aoc_image_repo={{the docker image you just pushed}}" \
  -var="aoc_version={{ the docker image tag name}}" \
  -var="testcase=../testcases/{{your test case folder name}}" \
  -var-file="../testcases/{{your test case folder name}}/parameters.tfvars" \
  -var="eks_cluster_name={{the eks cluster name in your account}}" \
  -var="deployment_type=fargate"
```

Don't forget to clean up your resources:
````
terraform destroy -auto-approve \
    -var="eks_cluster_name={{the eks cluster name in your account}}" \
    -var="deployment_type=fargate"
````
### 2.5 Run in soaking
**Prerequisite:** you are required to build aotutil for checking patch status
```
make build-aotutil
```
### 2.5.1 Run in soaking test
````
cd terraform/soaking && terraform init && terraform apply -auto-approve \
    -var="testing_ami={{ami need to test with such as soaking_window}}" \
    -var="aoc_image_repo={{the docker image you just pushed}}" \
    -var="aoc_version={{ the docker image tag name}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars" \
````

Don't forget to clean up your resources:
````
terraform destroy -auto-approve
````

### 2.5.2 Run in negative soaking test
````
cd terraform/soaking && terraform init && terraform apply -auto-approve \
    -var="negative_soaking=true" \
    -var="testing_ami={{ami need to test with such as soaking_window}}" \
    -var="aoc_image_repo={{the docker image you just pushed}}" \
    -var="aoc_version={{ the docker image tag name}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars" \
````

Don't forget to clean up your resources:
````
terraform destroy -auto-approve
````

### 2.6 Run in canary
````
cd terraform/canary && terraform init && terraform apply -auto-approve \
    -var="aoc_image_repo={{the docker image you just pushed}}" \
    -var="aoc_version={{ the docker image tag name}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
````
 
Don't forget to clean up your resources:
````
terraform destroy -auto-approve
````

### 2.7 Run batch tests

Batch testing allows a set of tests to be run synchronously. To do this,
a `test-case-batch` file is required in the `./terraform` directory. 
The format of the `test-case-batch` file is as such. 

```
serviceName1 testCase1 additionalValues1
serviceName2 testCase2 additionalValues2
serviceName3 testCase3 additionalValues3
serviceNameN testCaseN additionalValuesN
```

The values for these fields are as follows
`serviceName`: `EKS` `EKS-arm64` `EKS-fargate` `EKS-operator` `EKS-operator-arm64` `ECS` `EC2`
`testCase`: Must be an applicable test case in the `terraform/testcases` directory
`additionalValues`: For `EC2` tests it is expected that the `testing_ami` value is provided.
For ECS tests the `launch_type` variable is expected. For `EKS-arm64` and `EKS-operator-arm64` tests it is expected that
a pipe delimited string of `region|clustername|amp_endoint` is provided.

It is also expected that`TF_VAR_aoc_version` and `TF_VAR_aoc_image_repo` are set to valid values
pointing to a Collector image and repository to utilize. Default `aoc_image_repo` values can be utilized 
but the `TF_VAR_aoc_version` must be specified. 

To execute the test run
```
make exectute-batch-test
```

To clean up the successful test run cache
```
make postBatchClean
```



##3. Optional add-on
####3.1. Upload test case's terraform state to s3 bucket 
**Prerequisite:** you are required to run the test case before uploading any terraform state to s3
**Advantage:** Record what resources were created by test case and back-up in destroying those resources
when ```terraform destroy``` failed.
````
cd terraform/add_on/remote_state && terraform init && terraform apply \
   -var="testcase=../../testcases/{{your test case folder name}}" \
   -var="testing_id={{test case unique id}}" \
   -var="folder_name={{folder name when uploading to s3}} \
   -var="platform={{platform running (ec2, ecs, eks, canary,...)"\
````
