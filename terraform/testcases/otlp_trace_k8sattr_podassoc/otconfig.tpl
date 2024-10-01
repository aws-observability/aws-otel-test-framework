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
  otlphttp/proxy:
    endpoint: "http://localhost:4319"
  awsxray:

service:
  pipelines:
    traces/proxy:
      receivers: [otlp/proxy]
      processors: [batch]
      exporters: [otlphttp/proxy]
    traces/k8sattr:
      receivers: [otlp/k8sattr]
      processors: [k8sattributes,batch]
      exporters: [awsxray]