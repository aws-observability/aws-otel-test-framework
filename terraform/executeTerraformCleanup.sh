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
# $3: For EKS-arm64 we expect region|clustername|amp_endoint

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
    "EC2") TEST_FOLDER="./ec2/";
        opts+=" -var=testing_ami=$3";
    ;;
    "EKS") TEST_FOLDER="./eks/";
    ;;
    "EKS_ARM64") TEST_FOLDER="./eks/"
        arm_64_region=$(echo $3 | cut -d \| -f 1);
        arm_64_clustername=$(echo $3 | cut -d \| -f 2);
        arm_64_amp=$(echo $3 | cut -d \| -f 3);
        export AWS_REGION=${arm_64_region};
        opts+=" -var=region=${arm_64_region}";
        opts+=" -var=cortex_instance_endpoint=${arm_64_amp}";
        opts+=" -var=eks_cluster_name=${arm_64_clustername}";
    ;;
    "EKS_FARGATE") TEST_FOLDER="./eks/";
    ;;
    "EKS_ADOT_OPERATOR") TEST_FOLDER="./eks/";
        opts+=" -var=eks_cluster_name=adot-op-cluster";
    ;;
    "EKS_ADOT_OPERATOR_ARM64") TEST_FOLDER="./eks/";
        opts+=" -var=eks_cluster_name=arm64-adot-op-cluster";
    ;;
    "ECS") TEST_FOLDER="./ecs/";
        opts+=" -var=ecs_launch_type=$3";
    ;;
    *)
    echo "service ${service} is not valid";
    exit 1;
    ;;
esac

cd ${TEST_FOLDER};
case "$service" in
    "EKS_FARGATE" | "EKS_ARM64" | "EKS_ADOT_OPERATOR" | "EKS_ADOT_OPERATOR_ARM64") terraform destroy --auto-approve $opts;
    ;;
*)
    terraform destroy --auto-approve;
;;
esac
