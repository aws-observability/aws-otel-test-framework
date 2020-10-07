#!/bin/sh

running_type=${1}
platform=${2}
opts=${3}

echo ${opts}

validator_path="/app/validator/bin/validator"
terraform_root_path="/app/terraform"

case ${running_type} in
  validator)
    ${validator_path} "${opts}"
    ;;
  terraform)
    trap "cd ${terraform_root_path}/${platform} && terraform destroy -auto-approve ${opts}" HUP INT PIPE QUIT TERM EXIT SIGTERM SIGINT SIGQUIT SIGHUP ERR
    cd ${terraform_root_path}/${platform} && \
      terraform init && \
        terraform apply -auto-approve ${opts}
esac


