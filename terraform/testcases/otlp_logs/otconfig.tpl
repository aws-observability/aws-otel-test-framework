extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

processors:
  batch:

exporters:
  logging:
    verbosity: detailed
  awscloudwatchlogs:
    log_group_name: "/aws/ecs/aoc-testing-${testing_id}/OTLPLogs"
    log_stream_name: "otlp-logs"
    region: ${region}

service:
  pipelines:
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging, awscloudwatchlogs]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
