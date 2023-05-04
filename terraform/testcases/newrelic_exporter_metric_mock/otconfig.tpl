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
  newrelic:
    apikey: super-secret-api-key
    traces:
      host_override: ${mock_endpoint}
    metrics:
      host_override: ${mock_endpoint}

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging, newrelic]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
