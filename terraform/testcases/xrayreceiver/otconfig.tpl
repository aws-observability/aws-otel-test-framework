extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  awsxray:
    endpoint: 0.0.0.0:${udp_port}
    transport: udp

processors:
  batch:

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
      processors: [batch]
      exporters: [awsxray]
  extensions: [pprof]
