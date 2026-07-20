#!/bin/bash
# Starts kubectl port-forward for mocked-server and sample-app services.
# Called by terraform null_resource after deployments are ready.
# Usage: ./port-forward.sh <kubeconfig> <namespace> <app_port>

set -e

KUBECONFIG="$1"
NS="$2"
APP_PORT="$3"

export KUBECONFIG

# kubectl port-forward exits immediately when the service has no running pod
# behind it yet, so wait for ready endpoints before starting each forward.
wait_for_endpoints() {
  local svc="$1"
  local timeout="$2"
  local deadline=$((SECONDS + timeout))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if [ -n "$(kubectl -n "$NS" get endpoints "$svc" -o jsonpath='{.subsets[*].addresses[*].ip}' 2>/dev/null)" ]; then
      return 0
    fi
    sleep 5
  done
  return 1
}

start_forward() {
  local svc="$1"
  local mapping="$2"
  local pid_file="$3"
  local attempt
  for attempt in 1 2 3 4 5; do
    nohup kubectl -n "$NS" port-forward "svc/$svc" "$mapping" > "/tmp/pf_${svc}.log" 2>&1 &
    echo $! > "$pid_file"
    sleep 3
    if kill -0 "$(cat "$pid_file")" 2>/dev/null; then
      return 0
    fi
    echo "port-forward for $svc exited (attempt $attempt): $(tail -n 1 "/tmp/pf_${svc}.log")"
  done
  return 1
}

# The mocked-server service is not created for operator testcases.
if kubectl -n "$NS" get service mocked-server > /dev/null 2>&1; then
  wait_for_endpoints mocked-server 300 || echo "WARN: mocked-server has no ready endpoints"
  start_forward mocked-server 18081:80 /tmp/pf_mocked.pid || echo "WARN: mocked-server port-forward not ready"
fi

wait_for_endpoints sample-app 300 || echo "WARN: sample-app has no ready endpoints"
start_forward sample-app 18080:"$APP_PORT" /tmp/pf_sample.pid
