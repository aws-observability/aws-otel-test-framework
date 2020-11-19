receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

processors:
  batch/metrics:
    timeout: 60s

exporters:
  logging:
    loglevel: debug
  newrelic:
    apikey: super-secret-api-key
    metrics_url_override: "https://${mock_endpoint}"

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch/metrics]
      exporters: [logging, newrelic]
