#! /bin/bash

ecr_login_domain=${1}
ecr_image_name=${2}
build_home=${3}


 docker run --rm -it -v ~/.aws:/root/.aws amazon/aws-cli ecr get-login-password --region=us-west-2 | docker login --username AWS --password-stdin 611364707713.dkr.ecr.us-west-2.amazonaws.com
