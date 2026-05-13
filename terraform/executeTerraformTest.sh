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

set -o pipefail
set -x

ts() { date -u +"%H:%M:%S"; }
PROGRESS_FILE="${GITHUB_STEP_SUMMARY:-/dev/null}"

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
DESTROY_EXIT=0
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

CLEANUP_DONE=0
cleanup() {
    local rc=$?
    if [ "$CLEANUP_DONE" -eq 1 ]; then
        return
    fi
    echo "[cleanup] trap fired (rc=$rc), running terraform destroy"
    case "$SERVICE" in
        EKS*) terraform destroy --auto-approve $opts || true;
        ;;
        *)    terraform destroy --auto-approve            || true;
        ;;
    esac
    CLEANUP_DONE=1
    exit $rc
}
trap cleanup EXIT INT TERM

test_framework_shortsha=$(git rev-parse --short HEAD)
# Used as a retry mechanic.
ATTEMPTS_LEFT=1
cd ${TEST_FOLDER};

while [ $ATTEMPTS_LEFT -gt 0 ] && ! ../checkCacheHit.sh $SERVICE $TESTCASE $ADDTL_PARAMS; do
    TESTCASE_START=$(date -u +%s)
    echo "::group::${SERVICE} ${TESTCASE} ${ADDTL_PARAMS}"
    echo "[$(ts)] Starting: $SERVICE $TESTCASE $ADDTL_PARAMS"

    echo "[$(ts)] terraform init"
    terraform init;

    echo "[$(ts)] terraform apply (30m timeout)"

    if timeout -k 5m --signal=SIGINT -v 30m terraform apply -auto-approve -lock=false $opts  -var="testcase=../testcases/$TESTCASE" ; then
        APPLY_EXIT=$?
        DURATION=$(( $(date -u +%s) - TESTCASE_START ))
        echo "[$(ts)] Apply succeeded (${DURATION}s), writing cache entry"
        aws dynamodb put-item --region=us-west-2 --table-name ${DDB_TABLE_NAME} --item {\"TestId\":{\"S\":\"$SERVICE$TESTCASE$ADDTL_PARAMS\"}\,\"aoc_version\":{\"S\":\"$DDB_SK_PREFIX$test_framework_shortsha\"}\,\"TimeToExist\":{\"N\":\"${TTL_DATE}\"}} --return-consumed-capacity TOTAL
        echo "| $SERVICE | $TESTCASE | :white_check_mark: pass | ${DURATION}s |" >> "$PROGRESS_FILE"
    else
        APPLY_EXIT=$?
        DURATION=$(( $(date -u +%s) - TESTCASE_START ))
        echo "[$(ts)] Apply FAILED (exit=$APPLY_EXIT, ${DURATION}s)"
        echo "AWS_service: $SERVICE"
        echo "Testcase: $TESTCASE"
        echo "| $SERVICE | $TESTCASE | :x: fail (exit=$APPLY_EXIT) | ${DURATION}s |" >> "$PROGRESS_FILE"
    fi

    echo "[$(ts)] terraform destroy"

    case "$SERVICE" in
        EKS*) terraform destroy --auto-approve $opts;
              DESTROY_EXIT=$?;
        ;;
    *)
        terraform destroy --auto-approve;
        DESTROY_EXIT=$?;
    ;;
    esac

    echo "[$(ts)] Destroy complete (exit=$DESTROY_EXIT)"
    echo "::endgroup::"

    if [ $DESTROY_EXIT -ne 0 ]; then
        echo "[fatal] terraform destroy failed (exit=$DESTROY_EXIT), refusing to retry on broken state"
        CLEANUP_DONE=1
        exit $APPLY_EXIT
    fi

    if [ $APPLY_EXIT -ne 0 ]; then
        echo "Waiting 60s before retry to allow resource cleanup..."
        sleep 60
    fi

    let ATTEMPTS_LEFT=ATTEMPTS_LEFT-1
done

CLEANUP_DONE=1
exit $APPLY_EXIT
