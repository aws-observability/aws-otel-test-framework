extensions:
  pprof:
    endpoint: 0.0.0.0:1777
  sigv4auth:
    region: ${region}
    service: "aps"
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
  prometheusremotewrite:
    endpoint: ${cortex_instance_endpoint}/api/v1/remote_write
    timeout: 10s

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [awsprometheusremotewrite,prometheusremotewrite,logging]
  extensions: [pprof, sigv4auth]
  telemetry:
    logs:
      level: debug
