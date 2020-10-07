receivers:
  statsd:
    endpoint: "0.0.0.0:8125"
exporters:
  awsemf:
    namespace: '${otel_service_namespace}/${otel_service_name}'
    region: '${region}'
  logging:
    loglevel: debug
service:
  pipelines:
    metrics:
     receivers: [statsd]
     exporters: [awsemf, logging]