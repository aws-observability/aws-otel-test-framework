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
  metricstransform:
    transforms:
      - include: ".*${testing_id}"
        match_type: regexp
        action: update
        operations:
          - action: add_label
            new_label: "testingId"
            new_value: "${testing_id}"

exporters:
  logging:
    loglevel: debug
  awsemf:
    region: '${region}'

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [metricstransform, batch]
      exporters: [logging, awsemf]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
