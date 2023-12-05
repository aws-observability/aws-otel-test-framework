extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  awsecscontainermetrics:

processors:
  filter:
    metrics:
      include:
        match_type: strict
        metric_names:
          - ecs.task.memory.reserved
          - ecs.task.memory.utilized
          - ecs.task.cpu.reserved
          - ecs.task.cpu.utilized
          - ecs.task.network.rate.rx
          - ecs.task.network.rate.tx
          - ecs.task.storage.read_bytes
          - ecs.task.storage.write_bytes
  metricstransform:
    transforms:
      - include: ecs.task.memory.reserved
        action: update
        new_name: ecs.task.memory.reserved_${testing_id}
      - include: ecs.task.memory.utilized
        action: update
        new_name: ecs.task.memory.utilized_${testing_id}
      - include: ecs.task.cpu.reserved
        action: update
        new_name: ecs.task.cpu.reserved_${testing_id}
      - include: ecs.task.cpu.utilized
        action: update
        new_name: ecs.task.cpu.utilized_${testing_id}
      - include: ecs.task.network.rate.rx
        action: update
        new_name: ecs.task.network.rate.rx_${testing_id}
      - include: ecs.task.network.rate.tx
        action: update
        new_name: ecs.task.network.rate.tx_${testing_id}
      - include: ecs.task.storage.read_bytes
        action: update
        new_name: ecs.task.storage.read_bytes_${testing_id}
      - include: ecs.task.storage.write_bytes
        action: update
        new_name: ecs.task.storage.write_bytes_${testing_id}
  resource:
    attributes:
    - key: aws.ecs.docker.name
      action: delete
    - key: aws.ecs.task.pull_started_at
      action: delete
    - key: aws.ecs.task.pull_stopped_at
      action: delete
    - key: cloud.zone
      action: delete
    - key: aws.ecs.task.launch_type
      action: delete
    - key: cloud.region
      action: delete
    - key: cloud.account.id
      action: delete
    - key: container.id
      action: delete
    - key: container.name
      action: delete
    - key: container.image.name
      action: delete
    - key: aws.ecs.container.image.id
      action: delete
    - key: aws.ecs.container.exit_code
      action: delete
    - key: aws.ecs.container.created_at
      action: delete
    - key: aws.ecs.container.started_at
      action: delete
    - key: aws.ecs.container.finished_at
      action: delete
    - key: container.image.tag
      action: delete
    - key: aws.ecs.container.know_status
      action: delete
    - key: aws.ecs.task.known_status
      action: delete
      # We are normalizing this because of this issue: https://github.com/open-telemetry/opentelemetry-collector-contrib/issues/21412
      # In ECS running in Fargate this resource attribute is currently an empty-string.
    - key: aws.ecs.service.name
      action: upsert
      value: "NOOP"

exporters:
  awsemf:
    namespace: '${otel_service_namespace}/${otel_service_name}'
    region: '${region}'
    resource_to_telemetry_conversion:
      enabled: true

service:
  pipelines:
    metrics:
      receivers: [awsecscontainermetrics]
      processors: [filter, metricstransform, resource]
      exporters: [awsemf]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
