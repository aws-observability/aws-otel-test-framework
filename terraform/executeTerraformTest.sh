##########################################
# This script is used to execute a
# terraform test. A destroy will 
# alway be ran no matter if terraform was 
# successful or not. 
#
# ENV:
# TF_VAR_aoc_version
#
# Inputs
# $1: aws_service
# $2: testcase 
# $3: ECS/EC2 only - ami/ecs launch type 
# $3: For EKS-arm64 we expect region|clustername|amp_endoint
##########################################

set -e

echo $1
echo $2
echo $3


opts=""
if [[ -f ./testcases/${2}/parameters.tfvars ]] ; then 
    opts="-var-file=../testcases/${2}/parameters.tfvars" ; 
fi



US_EAST_2_AMP_ENDPOINT="https://aps-workspaces.us-east-2.amazonaws.com/workspaces/ws-1de68e95-0680-42bb-8e55-67e7fd5d0861";
APPLY_EXIT=0
TEST_FOLDER=""
REGION="us-west-2"
ADDITIONAL_VARS=""

service="${1}"
case "$service" in
    "EC2") TEST_FOLDER="./ec2/";
    ADDITIONAL_VARS="-var=\"testing_ami=${3}\"";
    ;;
    "EKS") TEST_FOLDER="./eks/";
    ;;
    "EKS-arm64") TEST_FOLDER="./eks/"
        arm_64_region=$(echo ${3} | cut -d \| -f 1);
        arm_64_clustername=$(echo ${3} | cut -d \| -f 2);
        arm_64_amp=$(echo ${3} | cut -d \| -f 3);
        ADDITIONAL_VARS="-var=\"region=${arm_64_region}\" -var=\"eks_cluster_name=${arm_64_clustername}\" -var=\"cortex_instance_endpoint=${arm_64_amp}\"";
    ;;
    "EKS-fargate") TEST_FOLDER="./eks/";
    ;;
    "EKS-operator") TEST_FOLDER="./eks/";
    ;;
    "ECS") TEST_FOLDER="./ecs/";
        ADDITIONAL_VARS="-var=\"ecs_launch_type=${3}\"";
    ;;
    *)
    echo "service ${service} is not valid";
    exit 1;
    ;;
esac

# create tmp dir for cache if doens't exist
[ ! -d "./tmp" ] && mkdir tmp

cd ${folder};
terraform init;
if [ ! -f "tmp/${2}" ]; then
    if terraform apply -auto-approve -lock=false $opts  -var="testcase=./testcases/$2" $ADDITIONAL_VARS ; then
        echo "Terraform apply failed"
        echo "AWS_service: ${1}"
        echo "Testcase: ${2}"
        echo "Additional var: ${ADDITIONAL_VARS}"
    else
        touch "tmp/${2}" 
    fi
fi


terraform destroy --auto-approve
