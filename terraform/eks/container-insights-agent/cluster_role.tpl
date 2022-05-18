kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: agent-role-${NAMESPACE}
rules:
  - apiGroups: [""]
    resources: ["pods", "nodes", "endpoints"]
    verbs: ["list", "watch", "get"]
  - apiGroups: ["apps"]
    resources: ["replicasets"]
    verbs: ["list", "watch", "get"]
  - apiGroups: ["batch"]
    resources: ["jobs"]
    verbs: ["list", "watch"]
  - apiGroups: [""]
    resources: ["nodes/proxy"]
    verbs: ["get"]
  - apiGroups: [""]
    resources: ["nodes/stats", "configmaps", "events"]
    verbs: ["create", "get"]
  - apiGroups: [""]
    resources: ["configmaps"]
    resourceNames: ["otel-container-insight-clusterleader"]
    verbs: ["get","update", "create"]
  - apiGroups:
    - coordination.k8s.io
    resourceNames:
    - otel-container-insight-clusterleader
    resources:
    - leases
    verbs:
    - get

