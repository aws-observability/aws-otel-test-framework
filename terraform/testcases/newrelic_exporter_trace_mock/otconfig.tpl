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
  newrelic:
    apikey: super-secret-api-key
    spans_url_override: "https://${mock_endpoint}"

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [newrelic]
  extensions: [pprof]
