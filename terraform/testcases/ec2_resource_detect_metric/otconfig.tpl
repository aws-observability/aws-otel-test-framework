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
  resourcedetection:
    detectors:
      - ec2
    timeout: 5s
    override: false
    ec2:
  resource:
    attributes:
    - key: host.id
      value: "123"
      action: update
    - key: host.image.id
      value: "1234"
      action: update
    - key: host.name
      value: "testname"
      action: update
    - key: host.type
      value: "testtype"
      action: update

exporters:
  logging:
    verbosity: detailed
  awsemf:
    region: '${region}'
    resource_to_telemetry_conversion:
      enabled: true
    dimension_rollup_option: "NoDimensionRollup"
    metric_declarations:
      - dimensions: [[host.id, host.image.id, host.name, host.type]]
        metric_name_selectors:
          - "latency_*"
          - "apiBytesSent_*"
          - "totalApiBytesSent_*"
          - "queueSizeChange_*"
          - "actualQueueSize_*"
          - "lastLatency_*"

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch, resourcedetection, resource]
      exporters: [logging, awsemf]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
