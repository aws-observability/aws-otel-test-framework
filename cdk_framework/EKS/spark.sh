export REGION="us-west-2"
export CLUSTER_CONFIG_PATH="./lib/config/cluster_config/clusters.yml"
export TESTCASE_CONFIG_PATH="./lib/config/test_case_config/spark-test-case.yml"
export CDK_EKS_RESOURCE_DEPLOY="true"
kubectl config set-context --current --namespace=collector-namespace