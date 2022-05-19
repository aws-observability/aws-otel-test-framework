kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: agent-role-${NAMESPACE}
rules:
  - apiGroups: [""]
    resources: ["pods", "nodes", "endpoints"]
    verbs: ["list", "watch"]
  - apiGroups: ["apps"]
    resources: ["replicasets"]
    verbs: ["list", "watch"]
  - apiGroups: ["batch"]
    resources: ["jobs"]
    verbs: ["list", "watch"]
  - apiGroups: [""]
    resources: ["nodes/proxy"]
    verbs: ["get"]
  - apiGroups: [""]
    resources: ["nodes/stats", "configmaps", "events", "leases"]
    verbs: ["create", "get", "update"]
  - apiGroups: [""]
    resources: ["configmaps", "leases"]
    resourceNames: ["otel-container-insight-clusterleader"]
    verbs: ["get","update", "create"]

