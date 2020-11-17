receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

processors:
  batch:
    timeout: 10s

exporters:
  logging:
    loglevel: debug
  datadog:
    api:
      key: testapikey
    metrics:
      endpoint: "https://${mock_endpoint}"

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [datadog]
