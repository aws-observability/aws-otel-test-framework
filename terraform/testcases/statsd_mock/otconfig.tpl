receivers:
  statsd:
    endpoint: 0.0.0.0:${udp_port}
    aggregation_interval: 60s
exporters:
  logging:
    loglevel: debug
  otlphttp:
    metrics_endpoint: "https://${mock_endpoint}"
    insecure: true
service:
  pipelines:
    metrics:
      receivers: [statsd]
      exporters: [otlphttp]