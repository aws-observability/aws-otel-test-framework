#! /bin/bash

module=${1}

cd ${module} && terraform destroy  -var-file="../common-config.tfvars"
