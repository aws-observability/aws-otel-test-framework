receivers:
  statsd:
    endpoint: 0.0.0.0:${udp_port}
    aggregation_interval: 20s
exporters:
  logging:
    loglevel: debug
  otlphttp:
    metrics_endpoint: "https://${mock_endpoint}"
    tls:
      insecure: true
service:
  pipelines:
    metrics:
      receivers: [statsd]
      exporters: [otlphttp]
  telemetry:
    logs:
      level: debug
