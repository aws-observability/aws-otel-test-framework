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
  logzio/traces:
    account_token: testToken
    endpoint: "https://${mock_endpoint}"

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [logzio]
  extensions: [pprof]
  telemetry:
    logs:
      level: debug
