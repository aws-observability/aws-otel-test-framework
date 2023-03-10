#!/bin/bash
##########################################
# This script is used to execute a terraform test case. It is primarily for use 
# with the Batch Test Generator. The script expects two to three arguments
# depending on the aws_service type. The script also expects certain enviornment 
# variables to be set. Expected inputs and env variables are listed below.
#
# Expected env variables:
# TF_VAR_aoc_version
# DDB_TABLE_NAME
# TTL_DATE time insert for TTL item in cache
# on mac TTL_DATE=$(date -v +7d +%s)
# for local use. command line vars will override this env var
# TF_VAR_cortex_instance_endpoint
# DDB_BATCH_CACHE_SK(OPTIONAL): If set then the prefix
# of the sortkey will be set to this value. The default value is the 
# value of TF_VAR_aoc_version. This is useful if testing
# something other than the ADOT Collector and you would like to use a different
# sort key. 
#
# Inputs
# $1: aws_service
# $2: testcase 
# $3: ECS/EC2 only - ami/ecs launch type 
# $3: For all EKS tests we expect region|clustername
##########################################

set -x

echo "Test Case Args: $@"
SERVICE="$1"
TESTCASE=$2
ADDTL_PARAMS=$3

if [[ -z "${DDB_BATCH_CACHE_SK}" ]]; then
    DDB_SK_PREFIX=$TF_VAR_aoc_version
else
    DDB_SK_PREFIX=$DDB_BATCH_CACHE_SK
fi
        
opts=""
if [[ -f ./testcases/$TESTCASE/parameters.tfvars ]] ; then 
    opts="-var-file=../testcases/$TESTCASE/parameters.tfvars" ; 
fi

APPLY_EXIT=0
TEST_FOLDER=""
export AWS_REGION=us-west-2
case "$SERVICE" in
    EC2) TEST_FOLDER="./ec2/";
        opts+=" -var=testing_ami=$ADDTL_PARAMS";
    ;;
    EKS*) TEST_FOLDER="./eks/"
        region=$(echo $ADDTL_PARAMS | cut -d \| -f 1);
        clustername=$(echo $ADDTL_PARAMS | cut -d \| -f 2);
        export AWS_REGION=${region};
        opts+=" -var=region=${region}";
        opts+=" -var=eks_cluster_name=${clustername}";
    ;;
    ECS) TEST_FOLDER="./ecs/";
        opts+=" -var=ecs_launch_type=$ADDTL_PARAMS";
    ;;
    *)
    echo "service ${SERVICE} is not valid";
    exit 1;
    ;;
esac

case ${AWS_REGION} in
    "us-east-2") export TF_VAR_cortex_instance_endpoint="https://aps-workspaces.us-east-2.amazonaws.com/workspaces/ws-1de68e95-0680-42bb-8e55-67e7fd5d0861";
    ;;
    "us-west-2") export TF_VAR_cortex_instance_endpoint="https://aps-workspaces.us-west-2.amazonaws.com/workspaces/ws-e0c3c74f-7fdf-4e90-87d2-a61f52df40cd";
    ;;
esac

test_framework_shortsha=$(git rev-parse --short HEAD)
# Used as a retry mechanic.
ATTEMPTS_LEFT=2
cd ${TEST_FOLDER};
while [ $ATTEMPTS_LEFT -gt 0 ] && ! ../checkCacheHit.sh $SERVICE $TESTCASE $ADDTL_PARAMS; do
    terraform init;
    if timeout -k 5m --signal=SIGINT -v 45m terraform apply -auto-approve -lock=false $opts  -var="testcase=../testcases/$TESTCASE" ; then
        APPLY_EXIT=$?
        echo "Exit code: $?" 
        aws dynamodb put-item --region=us-west-2 --table-name ${DDB_TABLE_NAME} --item {\"TestId\":{\"S\":\"$SERVICE$TESTCASE$ADDTL_PARAMS\"}\,\"aoc_version\":{\"S\":\"$DDB_SK_PREFIX$test_framework_shortsha\"}\,\"TimeToExist\":{\"N\":\"${TTL_DATE}\"}} --return-consumed-capacity TOTAL
    else
        APPLY_EXIT=$?
        echo "Terraform apply failed"
        echo "Exit code: $?"
        echo "AWS_service: $SERVICE"
        echo "Testcase: $TESTCASE" 
    fi

    case "$SERVICE" in
        EKS*) terraform destroy --auto-approve $opts;
        ;;
    *)
        terraform destroy --auto-approve;
    ;;
    esac

    let ATTEMPTS_LEFT=ATTEMPTS_LEFT-1
done


exit $APPLY_EXIT
