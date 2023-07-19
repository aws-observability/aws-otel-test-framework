receivers:
  otlp:
    protocols:
      grpc:

processors:
  k8sattributes:
  batch:

exporters:
  awsxray:


service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [k8sattributes, batch]
      exporters: [awsxray]