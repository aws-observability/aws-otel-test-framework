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

### 1.4 [Optional] Create a PR to [AOC Repo](https://github.com/aws-observability/aws-otel-collector) and record the version number

This is optional item, only do it if your goal is to add a component or fix a bug into the AOC Repo. Everytime when you create a PR to AOC repo, there will be a workflow to be running in this PR to do regression test and also, build testing artifacts[rpm, image, etc] for your code. Every PR will have a separate version number which you will be able to use it in the testing framework to verify whether your new code can pass your new testing suite.

1. create a branch in [AOC Repo](https://github.com/aws-observability/aws-otel-collector). [please don't fork at this moment, just create a branch directly in the AOC Repo]. [todo, after the repo becomes public, you can use fork instead of creating branch]

2. create a PR to merge the new branch to the `main` branch.

3. Waiting for the workflow checking in the PR to be finished.

4. find out the version number, click into the workflow page, click `e2etest-preparation` step, and click `Versioning for testing`, record the version number. Ex(v0.1.12-299946851).


## 2. Run ECS Test

### 2.1 run with the testing-suite

```shell
cd terraform/ecs && terraform init && terraform apply -var-file="../testing-suites/statsd-ecs.tfvars"
```

### 2.2 run with a specific aoc version if you have done 1.4

```shell
cd terraform/ecs && terraform init && terraform apply -var-file="../testing-suites/statsd-ecs.tfvars" -var="aoc_version={the version you got from workflow}"
```

### 2.3 don't forget to clean the resources

```shell
cd terraform/ecs && terraform destroy
```
## 3. Run EC2

### 3.1 run with the testing suite [support amazonlinux2, ubuntu16, windows2019]

```shell
cd terraform/ec2 && terraform init && terraform apply -var="testing_ami=amazonlinux2" -var="sshkey_s3_bucket={the bucket name you set in setup}" -var-file="../testing-suites/statsd-ec2.tfvars"
```

### 3.2 run with a specfic aoc version if you have done 1.4


```shell
cd terraform/ec2 && terraform init && terraform apply -var="sshkey_s3_bucket={the bucket name you set in setup}" -var-file="../testing-suites/statsd-ec2.tfvars" -var="aoc_version={the version you got from workflow}"
```

### 3.3 don't forget to clean the resources 
```shell
cd terraform/ec2 && terraform destroy
```

## 4. Run EKS

please note you are required to create a eks cluster in your account before running below command

### 4.1 run with the testing suite

```shell
cd terraform/eks && terraform init && terraform apply -var="eks_cluster_name={the eks cluster name in your account}" -var-file="../testing-suites/statsd-eks.tfvars"
```

### 4.2 run with a specfic aoc version if you have done 1.4

```shell
cd terraform/eks && terraform init && terraform apply -var="eks_cluster_name={the eks cluster name in your account}" -var-file="../testing-suites/statsd-eks.tfvars" -var="aoc_version={the version you got from workflow}"
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

