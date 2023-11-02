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
  prometheusremotewrite:
    endpoint: ${cortex_instance_endpoint}/api/v1/remote_write
    timeout: 10s
    auth:
      authenticator: sigv4auth

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheusremotewrite]
  extensions: [pprof, sigv4auth]
  telemetry:
    logs:
      level: ${log_level}
