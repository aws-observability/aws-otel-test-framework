receivers:
  statsd:
    endpoint: 0.0.0.0:${udp_port}
    aggregation_interval: 20s
exporters:
  awsemf:
    namespace: '${otel_service_namespace}/${otel_service_name}'
    region: '${region}'
  logging:
    verbosity: detailed
service:
  pipelines:
    metrics:
      receivers: [statsd]
      exporters: [awsemf, logging]
  telemetry:
    logs:
      level: ${log_level}
