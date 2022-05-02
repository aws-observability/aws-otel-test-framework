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
    endpoint: ${cortex_instance_endpoint}/api/v1/remote_write
    aws_auth:
      region: ${region}
      service: "aps"
    timeout: 10s

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
