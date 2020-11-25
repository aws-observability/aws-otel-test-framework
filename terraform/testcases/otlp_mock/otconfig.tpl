extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

exporters:
  logging:
    loglevel: debug
  awsxray:
    region: ${region}
    local_mode: true
    no_verify_ssl: false
    endpoint: "${mock_endpoint}"

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [logging, awsxray]
  extensions: [pprof]