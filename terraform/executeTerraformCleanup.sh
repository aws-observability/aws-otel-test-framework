#!/bin/bash
##########################################
# This script is used to destroy any
# leftover resources in the case where
# a GitHub actions workflow is cancelled. 
# There is a chance that this will fail due to
# the eks fargate and adot operator tests requiring 
# options to pass in. This is a best effort to clean up
# as many resources as possible though and any type
# of cleanup is better than cancelling with no cleanup. 
#
#
# Inputs
# $1: aws_service
# $2: testcase 
# $3: ECS/EC2 only - ami/ecs launch type 
# $3: For all EKS tests we expect region|clustername

##########################################

set -e
set -x

echo "Test Case Args: $@"


opts=""
if [[ -f ./testcases/$2/parameters.tfvars ]] ; then 
    opts="-var-file=../testcases/$2/parameters.tfvars" ; 
fi

APPLY_EXIT=0
TEST_FOLDER=""
service="$1"
export AWS_REGION=us-west-2
case "$service" in
    EC2) TEST_FOLDER="./ec2/";
        opts+=" -var=testing_ami=$3";
    ;;
    EKS*) TEST_FOLDER="./eks/"
        region=$(echo $3 | cut -d \| -f 1);
        clustername=$(echo $3 | cut -d \| -f 2);
        export AWS_REGION=${region};
        opts+=" -var=region=${region}";
        opts+=" -var=eks_cluster_name=${clustername}";
    ;;
    ECS) TEST_FOLDER="./ecs/";
        opts+=" -var=ecs_launch_type=$3";
    ;;
    *)
    echo "service ${service} is not valid";
    exit 1;
    ;;
esac

case ${AWS_REGION} in
    "us-east-2") export TF_VAR_cortex_instance_endpoint="https://aps-workspaces.us-east-2.amazonaws.com/workspaces/ws-1de68e95-0680-42bb-8e55-67e7fd5d0861";
    ;;
    "us-west-2") export TF_VAR_cortex_instance_endpoint="https://aps-workspaces.us-west-2.amazonaws.com/workspaces/ws-e0c3c74f-7fdf-4e90-87d2-a61f52df40cd";
    ;;
esac

cd ${TEST_FOLDER};
case "$service" in
    EKS*) terraform destroy --auto-approve $opts;
    ;;
*)
    terraform destroy --auto-approve;
;;
esac
