#!/bin/bash
##########################################
# This script is used to execute a
# terraform test. A destroy will 
# alway be ran no matter if terraform was 
# successful or not. 
#
# ENV:
# TF_VAR_aoc_version
# GITHUB_RUN_ID
# DDB_TABLE_NAME
# TTL_DATE time insert for TTL item in cache
# on mac TTL_DATE=$(date -v +7d +%s)
# for local use. command line vars will override this env var
# TF_VAR_cortex_instance_endpoint
#
# Inputs
# $1: aws_service
# $2: testcase 
# $3: ECS/EC2 only - ami/ecs launch type 
# $3: For EKS-arm64 we expect region|clustername|amp_endoint

##########################################

set -e
set -x

echo $@


opts=""
if [[ -f ./testcases/$2/parameters.tfvars ]] ; then 
    opts="-var-file=../testcases/$2/parameters.tfvars" ; 
fi

APPLY_EXIT=0
TEST_FOLDER=""
ADDITIONAL_VARS=""
service="$1"
export AWS_REGION=us-west-2
case "$service" in
    "EC2") TEST_FOLDER="./ec2/";
    ADDITIONAL_VARS=(-var=\"testing_ami=$3\");
    ;;
    "EKS") TEST_FOLDER="./eks/";
    ;;
    "EKS_ARM64") TEST_FOLDER="./eks/"
        arm_64_region=$(echo $3 | cut -d \| -f 1);
        arm_64_clustername=$(echo $3 | cut -d \| -f 2);
        arm_64_amp=$(echo $3 | cut -d \| -f 3);
        export AWS_REGION=${arm_64_region}
        export TF_VAR_region=${arm_64_region}
        export TF_VAR_cortex_instance_endpoint=${arm_64_amp}
        export TF_VAR_eks_cluster_name=${arm_64_clustername}
    ;;
    "EKS_FARGATE") TEST_FOLDER="./eks/";
    ;;
    "EKS_ADOT_OPERATOR") TEST_FOLDER="./eks/";
    ;;
    "ECS") TEST_FOLDER="./ecs/";
        ADDITIONAL_VARS=-var="ecs_launch_type=$3";
    ;;
    *)
    echo "service ${service} is not valid";
    exit 1;
    ;;
esac


CACHE_HIT=$(aws dynamodb get-item --region=us-west-2 --table-name ${DDB_TABLE_NAME} --key {\"TestId\":{\"S\":\"$1$2$3${TF_VAR_aoc_version}\"}})


if [ -z "${CACHE_HIT}" ]; then
    cd ${TEST_FOLDER};
    terraform init;
    if terraform apply -auto-approve -lock=false $opts  -var="testcase=../testcases/$2" ${ADDITIONAL_VARS} ; then
        echo "Exit code: $?"
        aws dynamodb put-item --region=us-west-2 --table-name ${DDB_TABLE_NAME} --item {\"TestId\":{\"S\":\"$1$2$3${TF_VAR_aoc_version}\"}\,\"TimeToExist\":{\"N\":\"${TTL_DATE}\"}} --return-consumed-capacity TOTAL
        terraform destroy --auto-approve
    else
        terraform destroy --auto-approve
        echo "Terraform apply failed"
        echo "Exit code: $?"
        echo "AWS_service: $1"
        echo "Testcase: $2"
        echo "Additional var: ${ADDITIONAL_VARS}"
        APPLY_EXIT=1
    fi
else
    echo "Cache Hit: ${CACHE_HIT}"
fi



exit $APPLY_EXIT
