extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  awsxray:
    endpoint: 0.0.0.0:${udp_port}
    transport: udp

exporters:
  logging:
    loglevel: debug
  awsxray:
    region: ${region}
    local_mode: true
    no_verify_ssl: false
    endpoint: "${mock_endpoint}"

service:
  extensions:
  pipelines:
    traces:
      receivers: [awsxray]
      exporters: [awsxray]
  extensions: [pprof]
