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
  awsprometheusremotewrite:
    endpoint: "https://${mock_endpoint}"
    aws_auth:
      region: "us-west-2"

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [awsprometheusremotewrite,logging]
  extensions: [pprof]
  telemetry:
    logs:
      level: debug
