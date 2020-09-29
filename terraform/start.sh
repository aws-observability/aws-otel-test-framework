#! /bin/bash

module=${1}

cd ${module} && terraform init && terraform destroy -var-file="../common-config.tfvars" -auto-approve && terraform apply -var-file="../common-config.tfvars" -auto-approve

