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
    token: ${TOKEN}