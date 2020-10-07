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

please add a new tfvars file under `terraform/testing-suites` folder.

### 4.1 add an ecs testing suite
specify below config in the tfvars file
1. otconfig_path, please put a new otconfig file under `terraform/templates/otconfig` folder and specify the path in the tfvars file.
2. ecs_taskdef_path[apply to ecs testing suite], please put a new ecs taskdef file under `terraform/templates/ecstaskdef` folder and specify the path in the tfvars file.
3. validation_config, please put a new validation config file under `validator/src/main/resources/validations` folder and specify the filename in the tfvars file.

### 4.2 add an ec2 testing suite [todo]

### 4.3 add an eks testing suite [todo]

## 5. configure the testing suite in github workflow
