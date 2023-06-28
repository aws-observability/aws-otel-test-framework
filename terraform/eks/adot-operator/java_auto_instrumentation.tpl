apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: my-instrumentation
  namespace: ${AOC_NAMESPACE}
spec:
  exporter:
    endpoint: http://aoc-collector:${GRPC_PORT}
  env:
   - name: OTEL_SERVICE_NAME
     value: ${SERVICE_NAME}
   - name: OTEL_RESOURCE_ATTRIBUTES
     value: "service.namespace=${SERVICE_NAMESPACE}"
  java:
    image: ${JAVA_AUTO_INSTRUMENTATION_REPOSITORY}:${JAVA_AUTO_INSTRUMENTATION_TAG}
