receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

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