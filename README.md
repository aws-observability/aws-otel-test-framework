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

## 2. Run ECS Test

### 2.1 run with the testing-suite

```shell
cd terraform/ecs && terraform init && terraform apply -var-file="../testing-suites/statsd-ecs.tfvars"
```

### 2.2 don't forget to clean the resources

```shell
cd terraform/ecs && terraform destory"
```
## 3. Run EC2

### 3.1 run with the default config [only amazonlinux2 is supported at this moment]

```shell
cd terraform/ec2 && terraform init && terraform apply -var-file="../testing-suites/statsd-ec2.tfvars"
```

### 3.2 don't forget to clean the resources
```shell
cd terraform/ec2 && terraform destory"
```

## 4. Add a testing suite

please check [adding a testing suite](terraform/README.md)

## 5. Contributing

We have collected notes on how to contribute to this project in CONTRIBUTING.md.

## 6. Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## 7. License

This project is licensed under the Apache-2.0 License.

