extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  jaeger:
    protocols:
      thrift_http:
        endpoint: 0.0.0.0:${http_port}

processors:
  batch:

exporters:
  logging:
    loglevel: debug
  otlphttp:
    traces_endpoint: "https://${mock_endpoint}"
    insecure: true


service:
  extensions:
  pipelines:
    traces:
      receivers: [jaeger]
      processors: [batch]
      exporters: [otlphttp, logging]
  extensions: [pprof]
