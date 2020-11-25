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
    local_mode: true
    region: '${region}'

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [awsxray]
  extensions: [pprof]
