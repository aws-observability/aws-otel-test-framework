# AOT Testing Framework
This project is a testing framework which could be used in both Otel SDK and AWS Otel Collector repo, it covers multiple aws platforms as well as multiple language sdk sample apps. 
provides a plugin framework for the contributors to add integration tests, which will automatically run in the workflows of AWS Otel Collector Repo.
The integration test workflow will generate traffic and send it through the collector to either a mock representing a 3rd party partner or an AWS endpoint.  

Before adding a new component into AWS Otel Collector, we require contributors to add related e2etest cases. 

## 1. Quick Start

#### checkout testing framework
````
git clone git@github.com:aws-observability/aws-otel-test-framework.git
````
#### checkout aws otel collector
````
git clone git@github.com:aws-observability/aws-otel-collector.git
````

### install terraform

please check https://learn.hashicorp.com/tutorials/terraform/install-cli

#### run one of the test cases
````shell script
cd aws-otel-test-framework/terraform/mock
terraform init
terraform apply -var="testcase=../testcases/otlp_mock" 
````

#### What does the test do?

this test

1. Builds collector image from the directory ../aws-otel-collector
2. Runs the collector, sample app and mock server in docker.
3. Validates if the mock server receives data from collector.

## 2. How to add a new test case?

### 2.1 Define test case

We define all the test cases under [the testcase directory](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform/testcases), and each sub folder will be treated as a test case. 

You will need to create a sub folder under [the testcase directory](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform/testcases), and place your configuration into it. Typically, 
you will need to place files as following(please use the same filename as below):

1. `otconfig.tpl`: which contain the new component, will be used as the config in all types of the e2etest. 
2. [optional] `parameters.tfvars`: you can run your test case locally with adding `-var-file=../testcases/yourtestcase/parameters.tfvars` to the terraform command. By using this file, you can override the default configuration in the testing framework. [The parameters you can override](terraform/README.md).
4. [optional] `docker_compose.tpl`: which is used to launch sample app container in ec2 test.
5. [optional] `ecs_taskdef.tpl`: which is used to launch ecs task in ecs test.
6. [optional] `eks_pod_config.tpl`: which is used to launch eks pod in eks test.

all the default files can be found [here](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform/templates/defaults)

### 2.2 Link test case

In the PR you create in [AWS Otel Collector](https://github.com/aws-observability/aws-otel-collector) to build the new component,
[link your testcase](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases.json)

When you link your testcase, there are five types of testing platforms you can choose,
* LOCAL, which will run the test in the pr workflow. 
* EC2, which will run the test in the main branch workflow under an ec2 instance.
* ECS, which will run the test in the main branch workflow under an ecs cluster.
* EKS, which will run the test in the main branch workflow under an eks/k8s cluster.
* SOAKING, which will run every night, and perform high throughput to your test case and monitor the resource usages.

Typically, we required all the types for every test case, if you have a special usage of your component which might not need a certain platform,
please put the reason in the pr.
If you find the current test case option can't fulfill your testing requirement, feel free to open an issue, so we can discuss together.

## 3. Run test cases in AWS platform

The commands in Quick Start is to run the test case in containers locally.
In the case that you want to debug for a certain platform, you can also use this testing framework to run your test case locally in multiple AWS platforms including EC2, ECS and EKS.

### 3.1 Prerequisite

#### 3.1.1 Setup your aws credentials
please check https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html

#### 3.1.2 Run Setup
Setup only needs to be run once, it creates

1. one iam role
2. one vpc
3. one security group
4. one s3 bucket which used to store the ssh key for ec2 login

run 
````
cd terraform/setup && terraform init && terraform apply
````

#### 3.1.3 Build Image
please follow https://github.com/aws-observability/aws-otel-collector/blob/main/docs/developers/build-docker.md to build your image with the new component, and push this image to dockerhub, record the image link, which will be used in your testing.

### 3.2 Run in ECS

````
cd terraform/ecs && terraform init && terraform apply -var="aoc_image_repo={{the docker image repo name you just pushed}}" -var="aoc_version={{ the docker image tag name}} -var="testcase=../testcases/{{your test case folder name}}" -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
````

don't forget to clean the resources
````
terraform destroy
````

### 3.3 Run in EKS
please note you are required to create a eks cluster in your account before running below command
````
cd terraform/eks && terraform init && terraform apply -var="eks_cluster_name={the eks cluster name in your account}" -var="aoc_version={{ the docker image tag name}}" -var="aoc_image_repo={{the docker image you just pushed}}" -var="testcase=../testcases/{{your test case folder name}}" -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
````

don't forget to clean the resources
````
terraform destroy
````

#### 3.4 Run in EC2 [TBD]

## 4. Contributing

We have collected notes on how to contribute to this project in CONTRIBUTING.md.

## 5. Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## 6. License

This project is licensed under the Apache-2.0 License.

