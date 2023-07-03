apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: my-instrumentation
  namespace: ${AOC_NAMESPACE}
spec:
  sampler:
    type: parentbased_traceidratio
    argument: "1.0"
  java:
    image: ${JAVA_AUTO_INSTRUMENTATION_REPOSITORY}:${JAVA_AUTO_INSTRUMENTATION_TAG}
