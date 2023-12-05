receivers:
  statsd:
    endpoint: 0.0.0.0:${udp_port}
    aggregation_interval: 20s
exporters:
  awsemf:
    namespace: '${otel_service_namespace}/${otel_service_name}'
    region: '${region}'
    dimension_rollup_option: "NoDimensionRollup"
    metric_declarations:
      - dimensions: [[mykey1, mykey2], [mykey1], [mykey2], [mykey4],[mykey5],[mykey3], []]
        metric_name_selectors:
          - "statsdTestMetric1g_*"
          - "statsdTestMetric1ms_*"
          - "statsdTestMetric1h_*"
          - "statsdTestMetric1c_*"

service:
  pipelines:
    metrics:
      receivers: [statsd]
      exporters: [awsemf]
  telemetry:
    logs:
      level: ${log_level}
