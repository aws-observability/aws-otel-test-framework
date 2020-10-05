#!/bin/sh
set -e

module=${1}
agent_version=${2}


cp terraform/common-config.tfvars /tmp/params.tfvars
echo "agent_version=\"${agent_version}\"" >> /tmp/params.tfvars
echo "validator_path=\"/app/validator/bin/validator\""

clean() {
  terraform destroy -var-file /tmp/params.tfvars -auto-approve
}
trap clean EXIT

cd terraform/${module} && \
terraform init -var-file /tmp/params.tfvars && \
terraform destroy -var-file /tmp/params.tfvars -auto-approve && \
terraform apply -var-file /tmp/params.tfvars -auto-approve && terraform destroy -var-file /tmp/params.tfvars

