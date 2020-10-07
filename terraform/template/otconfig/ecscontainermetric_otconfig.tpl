receivers:
    awsecscontainermetrics:
exporters:
    logging:
        loglevel: debug
    awsemf:
        namespace: '${otel_service_namespace}/${otel_service_name}'
        region: ‘us-west-2’
processors:
  filter:
    metrics:
      include:
        match_type: strict
        metric_names:
          - ecs.task.memory.utilized
          - ecs.task.memory.reserved
  metricstransform:
    transforms:
      - metric_name: ecs.task.memory.utilized
        action: update
        new_name: ecs.task.memory.utilized_${testing_id}

service:
    pipelines:
        metrics:
            receivers: [awsecscontainermetrics]
            processors: [filter, metricstransform]
            exporters: [logging, awsemf]