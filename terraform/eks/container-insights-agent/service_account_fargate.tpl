apiVersion: v1
kind: ServiceAccount
metadata:
  name: adot-collector-service-account
  namespace: default
  annotations:
    eks.amazonaws.com/role-arn: ${RoleArn}
