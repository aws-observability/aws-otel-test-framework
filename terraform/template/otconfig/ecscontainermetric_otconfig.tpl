receivers:
  awsecscontainermetrics:
exporters:
  logging:
    loglevel: debug
  awsemf:
    namespace: '${otel_service_namespace}/${otel_service_name}'
    region: 'us-west-2'
processors:
  filter:
    metrics:
      include:
        match_type: strict
        metric_names:
          - ecs.task.memory.utilized
          - ecs.task.memory.reserved
          - ecs.task.cpu.utilized
          - ecs.task.cpu.reserved
          - ecs.task.network.rate.rx
          - ecs.task.network.rate.tx
          - ecs.task.storage.read_bytes
          - ecs.task.storage.write_bytes
  metricstransform:
    transforms:
      - metric_name: ecs.task.memory.utilized
        action: update
        new_name: MemoryUtilized
      - metric_name: ecs.task.memory.reserved
        action: update
        new_name: MemoryReserved
      - metric_name: ecs.task.cpu.utilized
        action: update
        new_name: CpuUtilized
      - metric_name: ecs.task.cpu.reserved
        action: update
        new_name: CpuReserved
      - metric_name: ecs.task.network.rate.rx
        action: update
        new_name: NetworkRateRx
      - metric_name: ecs.task.network.rate.tx
        action: update
        new_name: NetworkRateTx
      - metric_name: ecs.task.storage.read_bytes
        action: update
        new_name: StorageRead
      - metric_name: ecs.task.storage.write_bytes
        action: update
        new_name: StorageWrite
service:
  pipelines:
    metrics:
      receivers: [awsecscontainermetrics]
      processors: [filter, metricstransform]
      exporters: [logging, awsemf]