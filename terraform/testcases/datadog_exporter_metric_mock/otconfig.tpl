extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

processors:
  batch:
    send_batch_size: 1
    timeout: 1s

exporters:
  datadog:
    api:
      key: abcdef
    metrics:
      endpoint: "https://${mock_endpoint}"

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [datadog]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
