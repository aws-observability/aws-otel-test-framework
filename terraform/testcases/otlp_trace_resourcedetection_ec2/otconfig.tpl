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
      detectors: [env, ec2]
      timeout: 2s
      override: true
      ec2:
        tags:
          - ^tag1$
          - ^tag2$

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
