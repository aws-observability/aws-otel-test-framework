# create cwagent service account
apiVersion: v1
kind: ServiceAccount
metadata:
  name: sa-${NAMESPACE}
  namespace: ${NAMESPACE}