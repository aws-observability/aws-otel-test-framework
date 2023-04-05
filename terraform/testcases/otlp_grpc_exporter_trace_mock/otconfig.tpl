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
  logging:
    verbosity: detailed
  otlp:
    endpoint: ${mock_endpoint}
    tls:
      insecure: true
    compression: none

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
