# Run Setup
Setup only needs to be run once, it creates 
1. one iam role
2. one vpc
3. one security group
4. one s3 bucket which used to store the ssh key for ec2 login

## run
``
cd setup && terraform init && terraform apply
``

# Run ECS

## run with the default config
``
cd ecs && terraform init && terraform apply
``

## run with the customized config
``
cd ecs && terraform init && terraform apply -var="ecs_taskdef_path=../template/ecstaskdef/default_ecs_taskdef.tpl" -var="otconfig_path=../template/otconfig/default_otconfig.tpl" -var="ecs_launch_type=FARGATE"
``

check ecs/variables.tf for more input vars.

# Run EC2

## run with the default config [only amazonlinux2 is supported at this moment]
``
cd ec2 && terraform init && terraform apply
``