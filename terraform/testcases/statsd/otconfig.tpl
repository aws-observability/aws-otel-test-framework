receivers:
  statsd:
    endpoint: 0.0.0.0:${udp_port}
    aggregation_interval: 60s
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