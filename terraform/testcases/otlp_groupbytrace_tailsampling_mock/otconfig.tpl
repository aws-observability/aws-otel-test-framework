extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

processors:
  groupbytrace:
    wait_duration: 2s
    num_traces: 20000
  tail_sampling:
    decision_wait: 1s
    policies:
      - name: latancy_based
        type: latency
        latency:
          threshold_ms: 200

exporters:
  awsxray:
    region: ${region}
    local_mode: true
    no_verify_ssl: false
    endpoint: "${mock_endpoint}"

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [groupbytrace, tail_sampling]
      exporters: [awsxray]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
