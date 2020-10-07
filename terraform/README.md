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
docker-compose up --build
``

## run with the customized config
change the docker-compose.yml

``
-var-file="/app/terraform/testing-suites/statsd-ecs.tfvars"
``

then run

``
docker-compose up --build
``

# Run EC2

## run with the default config [only amazonlinux2 is supported at this moment]
``
cd ec2 && terraform init && terraform apply
``