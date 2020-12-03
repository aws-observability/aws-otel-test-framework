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
    loglevel: debug
  sapm:
    endpoint: "https://${mock_endpoint}"

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [sapm]
  extensions: [pprof]
