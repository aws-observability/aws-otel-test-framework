receivers:
  prometheus:
    config:
      global:
        scrape_interval: 15s
      scrape_configs:
      - job_name: "test-prometheus-sample-app"
        static_configs:
        - targets: [ $SAMPLE_APP_HOST:$SAMPLE_APP_PORT ]
exporters:
  awsprometheusremotewrite:
    endpoint: ${cortex_instance_endpoint}/api/v1/remote_write
    aws_auth:
      region: ${region}
      service: "aps"
    timeout: 10s
  logging:
    loglevel: debug
service:
  pipelines:
    metrics:
     receivers: [prometheus]
     exporters: [awsprometheusremotewrite, logging]