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
  otlphttp:
    traces_endpoint: "https://${mock_endpoint}"
    tls:
      insecure: true


service:
  pipelines:
    traces:
      receivers: [jaeger]
      processors: [batch]
      exporters: [otlphttp]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
