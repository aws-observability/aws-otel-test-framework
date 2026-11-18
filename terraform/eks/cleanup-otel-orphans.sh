#!/bin/bash
##########################################
# Delete leftover OTel operator resources from previous test runs.
#
# A cancelled or crashed run can leave a Helm release behind with no
# terraform state pointing at it, so nothing ever uninstalls it. Its
# CRDs then block the next helm install with "invalid ownership
# metadata" errors.
#
# A resource is considered a leftover when the namespace in its
# meta.helm.sh/release-namespace annotation is missing, or exists but
# is older than STALE_AFTER_HOURS (a test namespace normally lives
# about 40 minutes, so an old one belongs to an abandoned run).
# Resources owned by a live release are never touched.
#
# Usage: ./cleanup-otel-orphans.sh <region> <cluster-name>
##########################################

REGION="$1"
CLUSTER="$2"
STALE_AFTER_HOURS="${STALE_AFTER_HOURS:-2}"

if [ -z "$REGION" ] || [ -z "$CLUSTER" ]; then
    echo "Usage: $0 <region> <cluster-name>"
    exit 1
fi

KUBECONFIG_FILE=$(mktemp)
trap 'rm -f "$KUBECONFIG_FILE"' EXIT

aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER" \
    --kubeconfig "$KUBECONFIG_FILE" > /dev/null 2>&1 || exit 0

K="kubectl --kubeconfig=$KUBECONFIG_FILE"
NOW=$(date -u +%s)

namespace_is_dead() {
    local created created_epoch
    created=$($K get namespace "$1" -o jsonpath='{.metadata.creationTimestamp}' 2>/dev/null) || return 0
    [ -n "$created" ] || return 0
    created_epoch=$(date -u -d "$created" +%s 2>/dev/null) || return 1
    [ $(( NOW - created_epoch )) -gt $(( STALE_AFTER_HOURS * 3600 )) ]
}

for kind in crd mutatingwebhookconfiguration validatingwebhookconfiguration clusterrole clusterrolebinding; do
    $K get "$kind" -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.annotations.meta\.helm\.sh/release-namespace}{"\n"}{end}' 2>/dev/null |
    grep opentelemetry | while read -r name ns; do
        [ -z "$name" ] && continue
        [ -z "$ns" ] && continue
        if namespace_is_dead "$ns"; then
            echo "  deleting $kind/$name (namespace '$ns' is missing or stale)"
            $K delete "$kind" "$name" --ignore-not-found 2>/dev/null || true
            $K delete namespace "$ns" --ignore-not-found --wait=false 2>/dev/null || true
        fi
    done
done
