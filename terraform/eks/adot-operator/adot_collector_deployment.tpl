apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: aoc
  namespace: ${AOC_NAMESPACE}
spec:
  image: public.ecr.aws/aws-observability/aws-otel-collector:latest
  mode: deployment
  serviceAccount: ${AOC_SERVICEACCOUNT}
  config: |
    ${AOC_CONFIG}
