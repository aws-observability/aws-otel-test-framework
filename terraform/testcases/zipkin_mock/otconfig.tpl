extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  zipkin:
    endpoint: 0.0.0.0:${http_port}

processors:
  batch:

exporters:
  logging:
    verbosity: detailed
  otlphttp:
    traces_endpoint: "https://${mock_endpoint}"
    tls:
      insecure: true


service:
  pipelines:
    traces:
      receivers: [zipkin]
      processors: [batch]
      exporters: [otlphttp,logging]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
