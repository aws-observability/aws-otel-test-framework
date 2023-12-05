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
  dynatrace:
    api_token: mytoken
    endpoint: "https://${mock_endpoint}"

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [dynatrace]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}

