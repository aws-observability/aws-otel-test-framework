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

### 3.1 run with the testing suite [only amazonlinux2 is supported at this moment]

```shell
cd terraform/ec2 && terraform init && terraform apply -var="sshkey_s3_bucket={the bucket name you set in setup}" -var-file="../testing-suites/statsd-ec2.tfvars"
```

### 3.2 don't forget to clean the resources
```shell
cd terraform/ec2 && terraform destory"
```

## 4. Run EKS

create a eks cluster in your account before run below command

### 4.1 run with the testing suite

```shell
cd terraform/eks && terraform init && terraform apply -var="eks_cluster_name={the eks cluster name in your account}" -var-file="../testing-suites/statsd-eks.tfvars"
```

### 4.3 don't forget to clean the resources

```
cd terraform/eks && terraform destroy
```

## 5. Add a testing suite

please check [adding a testing suite](terraform/README.md)

## 6. Contributing

We have collected notes on how to contribute to this project in CONTRIBUTING.md.

## 7. Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## 8. License

This project is licensed under the Apache-2.0 License.

