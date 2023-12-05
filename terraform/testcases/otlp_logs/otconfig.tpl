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
  awscloudwatchlogs:
    log_group_name: "/aws/ecs/otlp/${testing_id}/logs"
    log_stream_name: "otlp-logs"
    region: ${region}

service:
  pipelines:
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [awscloudwatchlogs]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
