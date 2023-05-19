apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: my-instrumentation
  namespace: ${AOC_NAMESPACE}
spec:
  exporter:
    endpoint: http://otc-container:4317
  propagators:
    - tracecontext
    - baggage
    - b3
  sampler:
    type: parentbased_traceidratio
    argument: "1.0"
  java:
    image: public.ecr.aws/aws-observability/adot-autoinstrumentation-java:1.24.0