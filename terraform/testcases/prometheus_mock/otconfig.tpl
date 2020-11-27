receivers:
  prometheus:
    config:
      global:
        scrape_interval: 15s
      scrape_configs:
      - job_name: "test-pipeline-job"
        static_configs:
        - targets: [ $SAMPLE_APP_HOST:$SAMPLE_APP_PORT ]
exporters:
  awsprometheusremotewrite:
    endpoint: "https://${mock_endpoint}"
service:
  pipelines:
    metrics:
     receivers: [prometheus]
     exporters: [awsprometheusremotewrite]
