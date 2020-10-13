receivers:
  awsecscontainermetrics:
exporters:
  logging:
    loglevel: debug
  awsemf:
    namespace: '${otel_service_namespace}/${otel_service_name}'
    region: '${region}'

service:
  pipelines:
    metrics:
      receivers: [awsecscontainermetrics]
      exporters: [logging, awsemf]