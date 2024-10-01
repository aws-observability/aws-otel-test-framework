receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

processors:
  k8sattributes:
  batch:

exporters:
  awsemf:
    resource_to_telemetry_conversion:
      enabled: true
    dimension_rollup_option: "NoDimensionRollup"
    metric_declarations:
      - dimensions: [[k8s.namespace.name, k8s.pod.name, k8s.pod.uid, k8s.deployment.name, k8s.node.name]]
        metric_name_selectors:
          - "latency_*"
          - "apiBytesSent_*"
          - "totalApiBytesSent_*"
          - "queueSizeChange_*"
          - "actualQueueSize_*"
          - "lastLatency_*"
  awsxray:


service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [k8sattributes,batch]
      exporters: [awsemf]
    traces:
      receivers: [otlp]
      processors: [k8sattributes, batch]
      exporters: [awsxray]