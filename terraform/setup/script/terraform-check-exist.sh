#Define environment variables
check_iam_role=true
check_iam_policy=false
check_ecr_repo=false
iam_role_name="aoc-e2e-iam-role"
function error_exit() {
  echo "$1" 1>&2
  exit 1
}

function parse_env_input() {

  if [[ ${check_iam_role} == "true" ]] && [[ -z "${iam_role_name}" ]]; then
      error_exit "Missing IAM role name when flag check_iam_role is true";
  fi

  if [[ ${check_iam_policy} == "true" ]] && [[ -z "${iam_policy_arn}" ]]; then
      error_exit "Missing IAM policy arn when flag check_iam_policy is true";
  fi

  if [[ ${check_ecr_repo} == "true" ]] && [[ -z "${ecr_repo_name}" ]]; then
      error_exit "Missing ECR repo name when flag check_ecr_repo is true";
  fi
}

function check_if_resources_exist() {
    if [[ ${check_iam_role} == "true" ]]; then
        aws iam wait role-exists --role-name "${iam_role_name}" > /dev/null || iam_role_exist=false
    fi

    if [[ ${check_iam_policy} == "true" ]]; then
        aws iam wait policy-exists --policy-arn "${iam_policy_arn}" > /dev/null || iam_policy_exist=false
    fi

    if [[ ${check_ecr_repo} == "true" ]]; then
        aws ecr describe-repositories --repository-names "${ecr_repo_name}" > /dev/null || ecr_repo_exist=false
    fi
}

function produce_output() {
  jq -n \
    --arg iam_role_exist "$iam_role_exist" \
    --arg iam_policy_exist "$iam_policy_exist" \
    --arg ecr_repo_exist "$ecr_repo_exist" \
    '{"iam_policy_exist":$iam_policy_exist,"iam_role_exist":$iam_role_exist,"ecr_repo_exist":$ecr_repo_exist}'
}

#Execute functions
parse_env_input
check_if_resources_exist
produce_output




