receivers:
  awsecscontainermetrics:
exporters:
  logging:
    loglevel: debug
  awsemf:
    namespace: '${otel_service_namespace}/${otel_service_name}'
    region: 'us-west-2'

service:
  pipelines:
    metrics:
      receivers: [awsecscontainermetrics]
      exporters: [logging, awsemf]