extensions:
  health_check:
receivers:
  prometheus:
    config:
      global:
        scrape_interval: 15s
      scrape_configs:
      - job_name: "test-prometheus-sample-app"
        static_configs:
        - targets: [ ${sample_app_listen_address_host}:${sample_app_listen_address_port} ]
exporters:
  logging:
service:
  pipelines:
    metrics:
     receivers: [prometheus]
     exporters: [logging]
  extensions: [health_check]
  telemetry:
    logs:
      level: ${log_level}
