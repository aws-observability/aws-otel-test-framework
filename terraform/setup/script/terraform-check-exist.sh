#Define environment variables
check_iam_role=false
check_iam_policy=false
check_ecr_repo=false

function error_exit() {
  echo "$1" 1>&2
  exit 1
}

function check_deps() {
  test -f $(which jq) || error_exit "jq command not detected in path, please install it"
  test -f $(which aws) || error_exit "aws command not detected in path, please install it"
}

function parse_env_input() {
  eval "$(jq -r '@sh "export check_sg=\(.check_sg) sg_name=\(.sg_name) check_iam_role=\(.check_iam_role) iam_role_name=\(.iam_role_name) check_iam_policy=\(.check_iam_policy) iam_policy_arn=\(.iam_policy_arn) check_ecr_repo=\(.check_ecr_repo) ecr_repo_name=\(.ecr_repo_name)"')"

  if [[ ${check_iam_role} == "true" ]] && [[ -z "${iam_role_name}" ]]; then
      error_exit "Missing IAM role name when flag check_iam_role is true";
  fi

  if [[ ${check_iam_policy} == "true" ]] && [[ -z "${iam_policy_arn}" ]]; then
      error_exit "Missing IAM policy arn when flag check_iam_policy is true";
  fi

  if [[ ${check_ecr_repo} == "true" ]] && [[ -z "${ecr_repo_name}" ]]; then
      error_exit "Missing ECR repo name when flag check_ecr_repo is true";
  fi

  if [[ ${check_sg} == "true" ]] && [[ -z "${sg_name}" ]]; then
      error_exit "Missing Security Group name when flag check_sg is true";
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

    if [[ ${check_sg} == "true" ]]; then
        aws ec2 wait security-group-exists --filters Name=group-name,Values="${sg_name}" > /dev/null || sg_exist=false
    fi
}

function produce_output() {
  jq -n \
    --arg iam_role_exist "$iam_role_exist" \
    --arg iam_policy_exist "$iam_policy_exist" \
    --arg ecr_repo_exist "$ecr_repo_exist" \
    --arg sg_exist "$sg_exist" \
    '{"iam_policy_exist":$iam_policy_exist,"iam_role_exist":$iam_role_exist,"ecr_repo_exist":$ecr_repo_exist,"sg_exist":$sg_exist}'
}

#Execute functions
check_deps
parse_env_input
check_if_resources_exist
produce_output









