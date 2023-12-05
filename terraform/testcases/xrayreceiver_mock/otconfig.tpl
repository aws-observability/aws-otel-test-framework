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
  awsxray:
    region: ${region}
    local_mode: true
    no_verify_ssl: false
    endpoint: "${mock_endpoint}"

service:
  pipelines:
    traces:
      receivers: [awsxray]
      processors: [batch]
      exporters: [awsxray]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
