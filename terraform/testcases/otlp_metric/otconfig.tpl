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
  awsemf:
    region: '${region}'

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch/metrics]
      exporters: [logging, awsemf]