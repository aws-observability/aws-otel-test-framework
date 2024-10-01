receivers:
  otlp/k8sattr:
    protocols:
      http:
        endpoint: "localhost:4319"
  otlp/proxy:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

processors:
  k8sattributes:
    pod_association:
      # below association matches for pair `k8s.pod.name` and `k8s.namespace.name`
      - sources:
          - from: resource_attribute
            name: k8s.pod.name
          - from: resource_attribute
            name: k8s.namespace.name
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
  otlphttp/proxy:
    endpoint: "http://localhost:4319"


service:
  pipelines:
    metrics/proxy:
      receivers: [otlp/proxy]
      processors: [batch]
      exporters: [otlphttp/proxy]
    metrics/k8sattr:
      receivers: [otlp/k8sattr]
      processors: [k8sattributes,batch]
      exporters: [awsemf]
