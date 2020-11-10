extensions:
  health_check:
  pprof:
    endpoint: 0.0.0.0:1777

receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:55680

processors:
  batch:
  queued_retry:

exporters:
  logging:
    loglevel: debug
  awsemf:
    region: '${region}'
    no_verify_ssl: false
    endpoint: "${mock_endpoint}"

service:
  pipelines:
    metrics:
      receivers: [otlp]
      exporters: [logging, awsemf]