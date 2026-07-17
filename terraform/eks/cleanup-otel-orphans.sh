#!/bin/bash
##########################################
# Delete leftover OTel operator cluster-scoped resources from previous
# test runs.
#
# Helm never deletes CRDs on uninstall, and cancelled runs lose the
# terraform state needed to destroy webhook configs and RBAC. These
# leftovers block the next helm install with "invalid ownership
# metadata" errors.
#
# A resource is considered a leftover when the namespace in its
# meta.helm.sh/release-namespace annotation no longer exists. Resources
# owned by a live release are never touched.
#
# Usage: ./cleanup-otel-orphans.sh <region> <cluster-name>
##########################################

REGION="$1"
CLUSTER="$2"

if [ -z "$REGION" ] || [ -z "$CLUSTER" ]; then
    echo "Usage: $0 <region> <cluster-name>"
    exit 1
fi

KUBECONFIG_FILE=$(mktemp)
trap 'rm -f "$KUBECONFIG_FILE"' EXIT

aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER" \
    --kubeconfig "$KUBECONFIG_FILE" > /dev/null 2>&1 || exit 0

K="kubectl --kubeconfig=$KUBECONFIG_FILE"

for kind in crd mutatingwebhookconfiguration validatingwebhookconfiguration clusterrole clusterrolebinding; do
    $K get "$kind" -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.annotations.meta\.helm\.sh/release-namespace}{"\n"}{end}' 2>/dev/null |
    grep opentelemetry | while read -r name ns; do
        [ -z "$name" ] && continue
        [ -z "$ns" ] && continue
        if ! $K get namespace "$ns" > /dev/null 2>&1; then
            echo "  deleting $kind/$name (namespace '$ns' is gone)"
            $K delete "$kind" "$name" --ignore-not-found 2>/dev/null || true
        fi
    done
done
