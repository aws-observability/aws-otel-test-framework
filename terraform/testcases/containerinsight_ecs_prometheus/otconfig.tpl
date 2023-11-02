# NOTE: cluster name for extension and emf exporter is configured using template var testing_id

extensions:
  ecs_observer:
    cluster_name: 'aoc-testing-${testing_id}'
    cluster_region: 'us-west-2'
    result_file: '/etc/ecs_sd_targets.yaml'
    refresh_interval: 15s
    job_label_name: prometheus_job
    # nginx https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights-Prometheus-Setup-nginx-ecs.html
    services:
      - name_pattern: '^.*nginx-service$'
        metrics_ports:
          - 9113
        job_name: nginx-prometheus-exporter
    # jmx
    docker_labels:
      - port_label: 'ECS_PROMETHEUS_EXPORTER_PORT'
    # appmesh, port and metrics are from envoy sidecar
    task_definitions:
      - arn_pattern: '.*:task-definition/.*-ColorTeller-(white):[0-9]+'
        metrics_path: '/stats/prometheus'
        metrics_ports:
          - 9901
        job_name: ecs-appmesh-color
      - arn_pattern: '.*:task-definition/.*-ColorGateway:[0-9]+'
        metrics_path: '/stats/prometheus'
        metrics_ports:
          - 9901
        job_name: ecs-appmesh-color

receivers:
  prometheus:
    config:
      scrape_configs:
        - job_name: "ecssd"
          file_sd_configs:
            - files:
                - '/etc/ecs_sd_targets.yaml'
          relabel_configs:
            - source_labels: [ __meta_ecs_cluster_name ] # ClusterName
              action: replace
              target_label: ClusterName
            - source_labels: [ __meta_ecs_service_name ] # ServiceName
              action: replace
              target_label: ServiceName
            - source_labels: [ __meta_ecs_task_definition_family ] # TaskDefinitionFamily
              action: replace
              target_label: TaskDefinitionFamily
            - source_labels: [ __meta_ecs_container_name ] # container_name
              action: replace
              target_label: container_name
            - action: labelmap # docker labels
              regex: ^__meta_ecs_container_labels_(.+)$
              replacement: '$$1'

exporters:
  awsemf:
    namespace: ECS/ContainerInsights/Prometheus
    # TODO: can we inject cluster name down from the pipeline
    log_group_name: "/aws/ecs/containerinsights/aoc-testing-${testing_id}/prometheus"
    # FIXME: we hard code the log stream name for now
    log_stream_name: 'ecssd'
    dimension_rollup_option: NoDimensionRollup
    metric_declarations:
      # nginx
      - dimensions: [ [ ClusterName, TaskDefinitionFamily, ServiceName ] ]
        label_matchers:
          - label_names:
              - ServiceName
            regex: '^.*nginx-service$'
        metric_name_selectors:
          - "^nginx_.*$"
      - dimensions: [ [ ClusterName, TaskDefinitionFamily, ServiceName ] ]
        label_matchers:
          - label_names:
              - ServiceName
            regex: '^.*nginx-plus-service$'
        metric_name_selectors:
          - "^nginxplus_connections_accepted$"
          - "^nginxplus_connections_active$"
          - "^nginxplus_connections_dropped$"
          - "^nginxplus_connections_idle$"
          - "^nginxplus_http_requests_total$"
          - "^nginxplus_ssl_handshakes$"
          - "^nginxplus_ssl_handshakes_failed$"
          - "^nginxplus_up$"
          - "^nginxplus_upstream_server_health_checks_fails$"
      - dimensions: [ [ ClusterName, TaskDefinitionFamily, ServiceName, upstream ] ]
        label_matchers:
          - label_names:
              - ServiceName
            regex: '^.*nginx-plus-service$'
        metric_name_selectors:
          - "^nginxplus_upstream_server_response_time$"
      - dimensions: [ [ ClusterName, TaskDefinitionFamily, ServiceName, code ] ]
        label_matchers:
          - label_names:
              - ServiceName
            regex: '^.*nginx-plus-service$'
        metric_name_selectors:
          - "^nginxplus_upstream_server_responses$"
          - "^nginxplus_server_zone_responses$"
      # jmx
      - dimensions: [ [ ClusterName, TaskDefinitionFamily, area ] ]
        label_matchers:
          - label_names:
              - Java_EMF_Metrics
            regex: ^true$
        metric_name_selectors:
          - "^jvm_memory_bytes_used$"
      - dimensions: [ [ ClusterName, TaskDefinitionFamily, pool ] ]
        label_matchers:
          - label_names:
              - Java_EMF_Metrics
            regex: ^true$
        metric_name_selectors:
          - "^jvm_memory_pool_bytes_used$"
      - dimensions: [ [ ClusterName, TaskDefinitionFamily ] ]
        label_matchers:
          - label_names:
              - Java_EMF_Metrics
            regex: ^true$
        metric_name_selectors:
          - "^jvm_threads_(current|daemon)$"
          - "^jvm_classes_loaded$"
          - "^java_lang_operatingsystem_(freephysicalmemorysize|totalphysicalmemorysize|freeswapspacesize|totalswapspacesize|systemcpuload|processcpuload|availableprocessors|openfiledescriptorcount)$"
          - "^catalina_manager_(rejectedsessions|activesessions)$"
          - "^jvm_gc_collection_seconds_(count|sum)$"
          - "^catalina_globalrequestprocessor_(bytesreceived|bytessent|requestcount|errorcount|processingtime)$"
      # AppMesh envoy
      - dimensions: [ [ "ClusterName","TaskDefinitionFamily" ] ]
        label_matchers:
          - label_names:
              - container_name
            regex: ^envoy$
        metric_name_selectors:
          - "^envoy_http_downstream_rq_(total|xx)$"
          - "^envoy_cluster_upstream_cx_(r|t)x_bytes_total$"
          - "^envoy_cluster_membership_(healthy|total)$"
          - "^envoy_server_memory_(allocated|heap_size)$"
          - "^envoy_cluster_upstream_cx_(connect_timeout|destroy_local_with_active_rq)$"
          - "^envoy_cluster_upstream_rq_(pending_failure_eject|pending_overflow|timeout|per_try_timeout|rx_reset|maintenance_mode)$"
          - "^envoy_http_downstream_cx_destroy_remote_active_rq$"
          - "^envoy_cluster_upstream_flow_control_(paused_reading_total|resumed_reading_total|backed_up_total|drained_total)$"
          - "^envoy_cluster_upstream_rq_retry$"
          - "^envoy_cluster_upstream_rq_retry_(success|overflow)$"
          - "^envoy_server_(version|uptime|live)$"
      - dimensions: [ [ "ClusterName","TaskDefinitionFamily","envoy_http_conn_manager_prefix","envoy_response_code_class" ] ]
        label_matchers:
          - label_names:
              - container_name
            regex: ^envoy$
        metric_name_selectors:
          - "^envoy_http_downstream_rq_xx$"


service:
  telemetry:
    logs:
      level: ${log_level}
  extensions: [ ecs_observer ]
  pipelines:
    metrics:
      receivers: [ prometheus ]
      exporters: [ awsemf ]
