#!/bin/bash
# Removes OTel CRDs and webhook configurations left by a prior abandoned
# operator Helm release so that the current install can proceed.
# Only deletes resources whose meta.helm.sh/release-name does NOT match
# the current testing_id, avoiding interference with a concurrent test.

set -euo pipefail

KUBECONFIG="$1"
CURRENT_RELEASE="adot-operator-$2"

export KUBECONFIG

OTEL_CRDS=$(kubectl get crds -o json 2>/dev/null \
  | jq -r --arg cur "$CURRENT_RELEASE" \
    '.items[]
     | select(.metadata.name | test("opentelemetry\\.io|opamp"))
     | select(.metadata.annotations["meta.helm.sh/release-name"] // "" | . != "" and . != $cur)
     | .metadata.name')

if [ -z "$OTEL_CRDS" ]; then
  echo "No stale OTel CRDs found"
  exit 0
fi

echo "Removing stale OTel CRDs:"
echo "$OTEL_CRDS"
echo "$OTEL_CRDS" | xargs kubectl delete crd --timeout=60s

STALE_OWNER=$(kubectl get crds -o json 2>/dev/null \
  | jq -r --arg cur "$CURRENT_RELEASE" \
    '.items[]
     | select(.metadata.name | test("opentelemetry\\.io|opamp"))
     | select(.metadata.annotations["meta.helm.sh/release-name"] // "" | . != "" and . != $cur)
     | .metadata.annotations["meta.helm.sh/release-name"]' \
  | sort -u | head -1)

if [ -n "$STALE_OWNER" ]; then
  echo "Removing webhooks and RBAC for stale release: $STALE_OWNER"
  kubectl delete mutatingwebhookconfiguration -l "app.kubernetes.io/instance=$STALE_OWNER" --timeout=30s 2>/dev/null || true
  kubectl delete validatingwebhookconfiguration -l "app.kubernetes.io/instance=$STALE_OWNER" --timeout=30s 2>/dev/null || true
  kubectl delete clusterrole,clusterrolebinding -l "app.kubernetes.io/instance=$STALE_OWNER" --timeout=30s 2>/dev/null || true
  kubectl delete namespace "${STALE_OWNER}-ns" --timeout=120s 2>/dev/null || true
fi

echo "Cleanup complete"
