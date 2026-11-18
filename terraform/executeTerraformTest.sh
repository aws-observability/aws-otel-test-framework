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

ts() { date -u +"%H:%M:%S"; }
PROGRESS_FILE="${GITHUB_STEP_SUMMARY:-/dev/null}"
if ! grep -q "Platform" "$PROGRESS_FILE" 2>/dev/null; then
  echo "| Platform | Test | Result | Duration |" >> "$PROGRESS_FILE"
  echo "|----------|------|--------|----------|" >> "$PROGRESS_FILE"
fi
test_framework_shortsha=$(git rev-parse --short HEAD)

# Pre-build validator image once (reused across all tests in this batch)
if ! docker image inspect aoc-validator:local > /dev/null 2>&1; then
  echo "[$(ts)] Building validator image..."
  docker build -t aoc-validator:local ../validator > /dev/null 2>&1
  echo "[$(ts)] Validator image built"
fi

# Used as a retry mechanic.
ATTEMPTS_LEFT=2
cd ${TEST_FOLDER};

TEST_COUNTER_FILE="/tmp/test_counter_${PPID}"
if [ -f "$TEST_COUNTER_FILE" ]; then
  TEST_INDEX=$(cat "$TEST_COUNTER_FILE")
else
  TEST_INDEX=0
fi
TEST_INDEX=$((TEST_INDEX + 1))
echo "$TEST_INDEX" > "$TEST_COUNTER_FILE"

ATTEMPT=0
while [ $ATTEMPTS_LEFT -gt 0 ] && ! ../checkCacheHit.sh $SERVICE $TESTCASE $ADDTL_PARAMS; do
    ATTEMPT=$((ATTEMPT + 1))
    TESTCASE_START=$(date -u +%s)
    RETRY_NOTE=""
    if [ $ATTEMPT -gt 1 ]; then RETRY_NOTE=" (RETRY #$((ATTEMPT-1)))"; fi
    echo ""
    echo "╔══════════════════════════════════════════════════════════════"
    echo "║ TEST ${TEST_INDEX}: ${SERVICE} / ${TESTCASE} / ${ADDTL_PARAMS}${RETRY_NOTE}"
    echo "╚══════════════════════════════════════════════════════════════"
    echo "::group::${SERVICE} ${TESTCASE} ${ADDTL_PARAMS}${RETRY_NOTE}"

    if [[ "$TESTCASE" == *"_adot_operator"* ]] && [ -f "./cleanup-otel-orphans.sh" ]; then
        echo "[$(ts)] cleanup-otel-orphans"
        ./cleanup-otel-orphans.sh "${region}" "${clustername}"
    fi

    echo "[$(ts)] terraform init"
    terraform init -no-color 2>&1 | tail -5

    echo "[$(ts)] terraform apply (30m timeout)"
    export TF_IN_AUTOMATION=true

    timeout -k 5m --signal=SIGINT -v 30m terraform apply -auto-approve -lock=false -compact-warnings $opts -var="testcase=../testcases/$TESTCASE" 2>&1 | tee /tmp/tf_apply.log | sed 's/\x1b\[[0-9;]*m//g' | grep --line-buffered -E "Creation complete|Error|Still creating.*[0-9]0s elapsed" || true
    TF_EXIT=${PIPESTATUS[0]}
    if [ $TF_EXIT -eq 0 ]; then
        APPLY_EXIT=0
        DURATION=$(( $(date -u +%s) - TESTCASE_START ))
        echo ""
        echo "  ✅ PASS: ${SERVICE} / ${TESTCASE} / ${ADDTL_PARAMS} (${DURATION}s)"
        echo ""
        aws dynamodb put-item --region=us-west-2 --table-name ${DDB_TABLE_NAME} --item {\"TestId\":{\"S\":\"$SERVICE$TESTCASE$ADDTL_PARAMS\"}\,\"aoc_version\":{\"S\":\"$DDB_SK_PREFIX$test_framework_shortsha\"}\,\"TimeToExist\":{\"N\":\"${TTL_DATE}\"}} --return-consumed-capacity TOTAL
        echo "| $SERVICE | $TESTCASE | :white_check_mark: pass | ${DURATION}s |" >> "$PROGRESS_FILE"
    else
        APPLY_EXIT=$TF_EXIT
        DURATION=$(( $(date -u +%s) - TESTCASE_START ))
        echo ""
        echo "  ❌ FAIL: ${SERVICE} / ${TESTCASE} / ${ADDTL_PARAMS} (exit=${APPLY_EXIT}, ${DURATION}s)"
        echo ""
        grep -E "Error:|validator" /tmp/tf_apply.log | tail -20
        echo "| $SERVICE | $TESTCASE | :x: fail (exit=$APPLY_EXIT) | ${DURATION}s |" >> "$PROGRESS_FILE"
    fi

    echo "[$(ts)] terraform destroy"
    case "$SERVICE" in
        EKS*) terraform destroy --auto-approve -compact-warnings $opts > /dev/null 2>&1;
        ;;
    *)
        terraform destroy --auto-approve -compact-warnings > /dev/null 2>&1;
    ;;
    esac

    echo "[$(ts)] Destroy complete"
    echo "::endgroup::"

    if [ $APPLY_EXIT -ne 0 ]; then
        echo "Waiting 10s before retry..."
        sleep 10
    fi

    let ATTEMPTS_LEFT=ATTEMPTS_LEFT-1
done


exit $APPLY_EXIT
