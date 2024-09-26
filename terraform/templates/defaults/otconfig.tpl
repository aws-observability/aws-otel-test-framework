extensions:
  health_check:
  pprof:
    endpoint: 0.0.0.0:1777

receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}
  awsxray:
    endpoint: 0.0.0.0:55690
    transport: udp

processors:
  batch/traces:
    timeout: 1s
    send_batch_size: 50
  batch/metrics:
    timeout: 60s

exporters:
  debug:
    verbosity: detailed
  awsxray:
    local_mode: true
    region: '${region}'
  awsemf:
    region: '${region}'

service:
  pipelines:
    traces:
      receivers: [otlp, awsxray]
      processors: [batch/traces]
      exporters: [debug, awsxray]
    metrics:
      receivers: [otlp]
      processors: [batch/metrics]
      exporters: [debug, awsemf]
  telemetry:
    logs:
      level: debug
