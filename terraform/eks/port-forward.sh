#!/bin/bash
# Starts kubectl port-forward for mocked-server and sample-app services.
# Includes keepalive monitoring to restart if port-forward drops.
# Usage: ./port-forward.sh <kubeconfig> <namespace> <app_port>

set -e

KUBECONFIG="$1"
NS="$2"
APP_PORT="$3"

export KUBECONFIG

# Wait for collector pod (operator-managed or direct deployment)
kubectl -n "$NS" wait --for=condition=ready pod -l app=aoc --timeout=300s 2>/dev/null || \
  kubectl -n "$NS" wait --for=condition=ready pod -l app.kubernetes.io/name=aoc-collector --timeout=300s 2>/dev/null || true

# Wait for sample app pod
kubectl -n "$NS" wait --for=condition=ready pod -l app=sample-app --timeout=300s 2>/dev/null || true

# Start port-forward with keepalive monitor
start_port_forward() {
  local SVC="$1" LOCAL_PORT="$2" REMOTE_PORT="$3" PID_FILE="$4" LOG_FILE="$5"

  # Kill existing if any
  kill $(cat "$PID_FILE" 2>/dev/null) 2>/dev/null || true

  nohup kubectl -n "$NS" port-forward "svc/$SVC" "$LOCAL_PORT:$REMOTE_PORT" > "$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
}

# Start a background keepalive monitor that checks and restarts port-forwards
start_keepalive() {
  (
    while true; do
      sleep 10
      # Check sample-app port-forward
      if ! kill -0 $(cat /tmp/pf_sample.pid 2>/dev/null) 2>/dev/null; then
        echo "[keepalive] sample-app port-forward died, restarting..." >> /tmp/pf_keepalive.log
        start_port_forward "sample-app" 18080 "$APP_PORT" /tmp/pf_sample.pid /tmp/pf_sample.log
        sleep 2
      fi
      # Check mocked-server port-forward
      if ! kill -0 $(cat /tmp/pf_mocked.pid 2>/dev/null) 2>/dev/null; then
        echo "[keepalive] mocked-server port-forward died, restarting..." >> /tmp/pf_keepalive.log
        start_port_forward "mocked-server" 18081 80 /tmp/pf_mocked.pid /tmp/pf_mocked.log
        sleep 2
      fi
    done
  ) &
  echo $! > /tmp/pf_keepalive.pid
}

# Initial start
start_port_forward "mocked-server" 18081 80 /tmp/pf_mocked.pid /tmp/pf_mocked.log
start_port_forward "sample-app" 18080 "$APP_PORT" /tmp/pf_sample.pid /tmp/pf_sample.log

# Start keepalive monitor
start_keepalive

# Wait for port forwards to establish
for i in $(seq 1 15); do
  if curl -sf http://localhost:18080/ > /dev/null 2>&1; then
    echo "INFO: sample-app port-forward ready"
    break
  fi
  sleep 2
done

curl -sf http://localhost:18080/ > /dev/null 2>&1 || echo "WARN: sample-app port-forward not ready after 30s"
