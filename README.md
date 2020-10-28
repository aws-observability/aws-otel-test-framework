# AOT Testing Framework
This project is a testing framework which could be used in both SDK repo and AOC repo, it covers multiple aws platforms as well as multiple language sdk sample apps. 

before adding a new component into AOC, we require contributor to add a new testing suite in this framework.

## 1. Prerequisite

### 1.1 Setup your aws credentials

please check https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html

### 1.2 install terraform

please check https://learn.hashicorp.com/tutorials/terraform/install-cli

### 1.3 Run Setup
Setup only needs to be run once, it creates 
1. one iam role
2. one vpc
3. one security group
4. one s3 bucket which used to store the ssh key for ec2 login

run
``
cd terraform/setup && terraform init && terraform apply
``

### 1.4 Build Image

please follow https://github.com/aws-observability/aws-otel-collector/blob/main/docs/developers/build-docker.md to build your image with the new component, and push this image to dockerhub, record the image link, which will be used in your testing.


## 2. Run ECS Regression Test

### 2.1 run with the testing-suite

```shell
cd terraform/ecs && terraform init && terraform apply -var-file="../testing-suites/statsd-ecs.tfvars" -var="aoc_image_repo={{the docker image you just pushed}}"
```

### 2.2 don't forget to clean the resources

```shell
cd terraform/ecs && terraform destroy
```

## 3. Run EKS Regression Test

please note you are required to create a eks cluster in your account before running below command

### 3.1 run with the testing suite

```shell
cd terraform/eks && terraform init && terraform apply -var="eks_cluster_name={the eks cluster name in your account}" -var-file="../testing-suites/statsd-eks.tfvars" -var="aoc_image_repo={{the docker image you just pushed}}"
```

### 3.2 don't forget to clean the resources

```
cd terraform/eks && terraform destroy
```

## 4. Add a testing suite

please check [adding a testing suite](terraform/README.md)

## 5. Contributing

We have collected notes on how to contribute to this project in CONTRIBUTING.md.

## 7. Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## 8. License

This project is licensed under the Apache-2.0 License.

