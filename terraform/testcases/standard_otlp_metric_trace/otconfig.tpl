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
    verbosity: detailed
  awsemf:
    region: ${region}
    no_verify_ssl: false
  awsxray:
    region: ${region}
    local_mode: true
    no_verify_ssl: false

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging, awsemf]
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging, awsxray]
  extensions: [pprof]
  telemetry:
    logs:
      level: debug
