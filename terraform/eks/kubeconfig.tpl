apiVersion: v1
clusters:
- cluster:
    certificate-authority-data: ${CA_DATA}
    server: ${SERVER_ENDPOINT}
  name: testing_cluster
contexts:
- context:
    cluster: testing_cluster
    user: terraform_user
  name: integ_test
current-context: integ_test
kind: Config
preferences: {}
users:
- name: terraform_user
  user:
    exec:
      apiVersion: client.authentication.k8s.io/v1beta1
      command: aws
      args:
        - eks
        - get-token
        - --cluster-name
        - ${CLUSTER_NAME}
        - --region
        - ${REGION}