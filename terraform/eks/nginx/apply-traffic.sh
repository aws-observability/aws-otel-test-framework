#!/bin/bash
# Waits for nginx LB hostname and applies traffic deployment.
# Environment variables: KUBECONFIG, NS, SVC, NAMESPACE
set -e

for i in $(seq 1 150); do
  EXTERNAL_IP=$(kubectl --kubeconfig="$KUBECONFIG" get svc -n"$NS" "$SVC" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)
  if [ -n "$EXTERNAL_IP" ]; then break; fi
  sleep 2
done

if [ -z "$EXTERNAL_IP" ]; then
  echo "Error: nginx LB hostname not available after 300s"
  exit 1
fi

echo "Nginx LB: $EXTERNAL_IP"
export NAMESPACE EXTERNAL_IP
envsubst '${NAMESPACE} ${EXTERNAL_IP}' < ./nginx/nginx_traffic_sample.tpl > /tmp/nginx_traffic.yml

# Retry kubectl apply — admission webhook may not be ready immediately after Helm install
for i in $(seq 1 10); do
  if kubectl --kubeconfig="$KUBECONFIG" apply -f /tmp/nginx_traffic.yml 2>/dev/null; then
    exit 0
  fi
  echo "Waiting for nginx admission webhook to be ready... (attempt $i/10)"
  sleep 5
done
echo "Error: kubectl apply failed after 10 retries"
kubectl --kubeconfig="$KUBECONFIG" apply -f /tmp/nginx_traffic.yml
