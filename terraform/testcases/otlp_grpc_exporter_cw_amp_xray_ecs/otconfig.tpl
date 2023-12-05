extensions:
  pprof:
    endpoint: 0.0.0.0:1777
  sigv4auth:
    region: ${region}
    service: "aps"

receivers:
  awsecscontainermetrics:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

processors:
  batch:
    send_batch_size: 50

  resourcedetection:
    detectors: [ env, ecs ]
    timeout: 2s
    override: false

  #Only include some metrics relating to the container's metrics
  #Documentation: https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/processor/filterprocessor
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

  #Update metrics' name to be equal to the expected container metrics in mustache
  #Expected metric: https://github.com/aws-observability/aws-otel-test-framework/blob/terraform/validator/src/main/resources/expected-data-template/ecsContainerExpectedMetric.mustache
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
  #Delete some metrics' dimension exporting to the cloudwatch or amp
  #Documentation: https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/processor/resourceprocessor
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
      - key: aws.ecs.service.name
        action: upsert
        value: "NOOP"

exporters:
  awsemf:
    namespace: '${otel_service_namespace}/${otel_service_name}'
    region: '${region}'
    resource_to_telemetry_conversion:
      enabled: true
  prometheusremotewrite:
    endpoint: ${cortex_instance_endpoint}/api/v1/remote_write
    resource_to_telemetry_conversion:
      enabled: true
    auth:
      authenticator: sigv4auth
  awsxray:
    local_mode: true
    region: '${region}'

service:
  pipelines:
    metrics/container/cw:
      receivers: [awsecscontainermetrics]
      processors: [ filter, metricstransform, resource, batch]
      exporters: [awsemf]
    metrics/container/amp:
      receivers: [ awsecscontainermetrics ]
      processors: [ filter, metricstransform, resource, batch ]
      exporters: [ prometheusremotewrite]
    metrics/application/cw:
      receivers: [ otlp ]
      processors: [ resourcedetection, batch ]
      exporters: [ awsemf]
    metrics/application/amp:
      receivers: [ otlp ]
      processors: [ resourcedetection, batch ]
      exporters: [ prometheusremotewrite]
    traces/application/xray:
      receivers: [ otlp ]
      processors: [ resourcedetection, batch ]
      exporters: [ awsxray ]
  extensions: [pprof, sigv4auth]
  telemetry:
    logs:
      level: ${log_level}
