receivers:
  awscontainerinsightreceiver:
    collection_interval: 10s
    container_orchestrator: ecs

processors:
  batch/metrics:
    timeout: 20s

exporters:
  awsemf:
    namespace: ContainerInsightsEC2Instance
    log_group_name: '/aws/ecs/containerinsights/{ClusterName}/performance'
    log_stream_name: 'instanceTelemetry/{ContainerInstanceId}'
    resource_to_telemetry_conversion:
      enabled: true
    dimension_rollup_option: NoDimensionRollup
    parse_json_encoded_attr_values: [Sources]
    metric_declarations:
      # instance metrics
      - dimensions: [ [ ContainerInstanceId, InstanceId, ClusterName] ]
        metric_name_selectors:
          - instance_cpu_utilization
          - instance_memory_utilization
          - instance_network_total_bytes
          - instance_cpu_reserved_capacity
          - instance_memory_reserved_capacity
          - instance_number_of_running_tasks
          - instance_filesystem_utilization
      - dimensions: [ [ClusterName] ]
        metric_name_selectors:
          - instance_cpu_utilization
          - instance_memory_utilization
          - instance_network_total_bytes
          - instance_cpu_reserved_capacity
          - instance_memory_reserved_capacity
          - instance_number_of_running_tasks
          - instance_cpu_usage_total
          - instance_cpu_limit
          - instance_memory_working_set
          - instance_memory_limit
service:
  pipelines:
    metrics:
      receivers: [awscontainerinsightreceiver]
      processors: [batch/metrics]
      exporters: [awsemf]
  telemetry:
    logs:
      level: ${log_level}
