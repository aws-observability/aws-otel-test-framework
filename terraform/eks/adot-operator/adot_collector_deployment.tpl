apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: aoc
  namespace: ${AOC_NAMESPACE}
spec:
  image: ${AOC_IMAGE}
  mode: ${AOC_DEPLOY_MODE}
  serviceAccount: ${AOC_SERVICEACCOUNT}
  config: |
    ${AOC_CONFIG}
