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

exporters:
  datadog:
    api:
      key: testapikey
    metrics:
      endpoint: "https://${mock_endpoint}"
    traces:
      endpoint: "https://${mock_endpoint}"

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [datadog]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
