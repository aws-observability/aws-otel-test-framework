receivers:
  awsxray:
    endpoint: 0.0.0.0:2000
    transport: udp
exporters:
  logging:
    loglevel: debug
  awsxray:
    region: ${region}
service:
  extensions:
  pipelines:
    traces:
      receivers: [awsxray]
      exporters: [logging, awsxray]