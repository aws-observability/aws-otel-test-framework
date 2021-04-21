# create cluster role binding for service account
kind: ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: agent-role-binding-${NAMESPACE}
subjects:
  - kind: ServiceAccount
    name: ${SERVICE_ACCOUNT}
    namespace: ${NAMESPACE}
roleRef:
  kind: ClusterRole
  name: agent-role-${NAMESPACE}
  apiGroup: rbac.authorization.k8s.io