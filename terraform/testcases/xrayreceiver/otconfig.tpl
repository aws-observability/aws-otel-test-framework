receivers:
  awsxray:
    endpoint: 0.0.0.0:${udp_port}
    transport: udp

exporters:
  logging:
    loglevel: debug
  awsxray:
    local_mode: true
    region: '${region}'

service:
  pipelines:
    traces:
      receivers: [awsxray]
      exporters: [logging, awsxray]
