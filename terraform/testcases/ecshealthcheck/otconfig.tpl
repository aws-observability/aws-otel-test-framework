extensions:
  health_check:
    endpoint: "localhost:13133"
    path: "/health/status"
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
      loglevel: debug
service:
  pipelines:
    metrics:
     receivers: [prometheus]
     exporters: [logging]
  extensions: [health_check]
  telemetry:
    logs:
      level: debug
