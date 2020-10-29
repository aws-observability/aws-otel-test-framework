# AOT Testing Framework
This project is a testing framework which could be used in both SDK repo and AOC repo, it covers multiple aws platforms as well as multiple language sdk sample apps. 

before adding a new component into AWS Otel Collector, we require contributors to add related e2etest cases. 

## 1. How do I add a new OT component[receiver, processor, exporter] into AWS Otel Collector? 

There're two requirements:

### 1.1 Open a pr to [the testing framework] (https://github.com/aws-observability/aws-otel-test-framework) repo to add a testcase for your component.

all the test cases are defined under [the testcase directory](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform/testcases), and each sub folder will be treated as a test case. 

You will need to create a sub folder under [the testcase directory](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform/testcases), and place your configuration into it. Typically, you will need to place files as following(please use the same filename as below):

1. `otconfig.tpl`: which contain the new component, will be used as the config in the e2etest. 
2. [optional] `parameters.tfvars`: by using this file, you can override the default configuration in the testing framework. [The parameters you can override](terraform/README.md). 
3. [optional] `supported_platforms`: if this file is NOT presented, it means this test case will be running in all the platforms (EC2, ECS, EKS). If this file is presented, only the platform defined inside will be applied to the test case. [For example](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases/ecsmetrics/supported_platforms). 
4. [optional] `docker_compose.tpl`, which is used to launch sample app container in ec2 test.
5. [optional] `ecs_taskdef.tpl`, which is used to launch ecs task in ecs test.
6. [optional] `eks_pod_config.tpl`, which is used to launch eks pod in eks test.

all the default files can be found [here] (https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform/templates/defaults)

If you find the current test case option can not fulfill your testing requirement, feel free to open an issue here so we can discuss together.

### 1.2 Open a PR to [AWS Otel Collector](https://github.com/aws-observability/aws-otel-collector) to build the new component

You need to create a PR in [AWS Otel Collector](https://github.com/aws-observability/aws-otel-collector), "link" the testcase, and get it approved and merged. Then we will schedule to release a new version of collector with the new component.

three files you need to focus on, 

* [go module to add your component](https://github.com/aws-observability/aws-otel-collector/blob/main/go.mod)
* [enable your component](https://github.com/aws-observability/aws-otel-collector/blob/main/pkg/defaultcomponents/defaults.go).
* [link your testcase] (https://github.com/aws-observability/aws-otel-collector/e2etest/testcases.txt)

## 2. Run and debug your testcase

You can run the testcase on your local. follow the below guide to set up testing framework on your local.

### 2.1 Prerequisite

#### 2.1.1 Setup your aws credentials

please check https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html

#### 2.1.2 install terraform

please check https://learn.hashicorp.com/tutorials/terraform/install-cli

#### 2.1.3 Run Setup
Setup only needs to be run once, it creates 
1. one iam role
2. one vpc
3. one security group
4. one s3 bucket which used to store the ssh key for ec2 login

run
``
cd terraform/setup && terraform init && terraform apply
``

#### 2.1.4 Build Image

please follow https://github.com/aws-observability/aws-otel-collector/blob/main/docs/developers/build-docker.md to build your image with the new component, and push this image to dockerhub, record the image link, which will be used in your testing.


### 2.2 Run Test locally

#### 2.2.1 ECS

```shell
cd terraform/ecs && terraform init && terraform apply -var="aoc_image_repo={{the docker image repo name you just pushed}}" -var="aoc_version={{ the docker image tag name}} -var="testcase=../testcases/{{your test case folder name}}" -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
```

don't forget to clean the resources

```shell
cd terraform/ecs && terraform destroy
```

#### 2.2.2 EKS

please note you are required to create a eks cluster in your account before running below command

```shell
cd terraform/eks && terraform init && terraform apply -var="eks_cluster_name={the eks cluster name in your account}" -var="aoc_version={{ the docker image tag name}}" -var="aoc_image_repo={{the docker image you just pushed}}" -var="testcase=../testcases/{{your test case folder name}}" -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
```

don't forget to clean the resources

```
cd terraform/eks && terraform destroy
```

### 2.3 Parameters override

Bascially, you will need to add a parameter file to overide the default values with `-var-file=xxx.tfvars` 

please check [the parameters to override](terraform/README.md)

## 3. Contributing

We have collected notes on how to contribute to this project in CONTRIBUTING.md.

## 4. Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## 5. License

This project is licensed under the Apache-2.0 License.

