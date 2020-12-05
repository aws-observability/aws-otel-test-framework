extensions:
  pprof:
    endpoint: 0.0.0.0:1777
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
  otlp:
    endpoint: ${mock_endpoint}
    insecure: true

service:
  pipelines:
    metrics:
      receivers: [otlp]
      exporters: [otlp]
  extensions: [pprof]