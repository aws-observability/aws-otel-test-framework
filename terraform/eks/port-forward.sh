#!/bin/bash
# Starts kubectl port-forward for mocked-server and sample-app services.
# Called by terraform null_resource after deployments are ready.
# Usage: ./port-forward.sh <kubeconfig> <namespace> <app_port>

set -e

KUBECONFIG="$1"
NS="$2"
APP_PORT="$3"

export KUBECONFIG

kubectl -n "$NS" wait --for=condition=ready pod -l app=aoc --timeout=300s || true

nohup kubectl -n "$NS" port-forward svc/mocked-server 18081:80 > /dev/null 2>&1 &
echo $! > /tmp/pf_mocked.pid

nohup kubectl -n "$NS" port-forward svc/sample-app 18080:"$APP_PORT" > /dev/null 2>&1 &
echo $! > /tmp/pf_sample.pid

sleep 2
curl -sf http://localhost:18081/ > /dev/null || echo "WARN: mocked-server port-forward not ready"
