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
  resourcedetection:
      detectors: [env, ecs]
      timeout: 2s
      override: false

exporters:
  awsxray:
    local_mode: true
    region: '${region}'

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [resourcedetection, batch]
      exporters: [awsxray]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
