receivers:
  prometheus:
    config:
      global:
        scrape_interval: 15s
      scrape_configs:
      - job_name: "test-pipeline-job"
        static_configs:
        - targets: [ ${sample_app_listen_address_host}:${sample_app_listen_address_port} ]
exporters:
  awsprometheusremotewrite:
    endpoint: "https://${mock_endpoint}"
    aws_auth:
      region: "us-west-2"
service:
  pipelines:
    metrics:
     receivers: [prometheus]
     exporters: [awsprometheusremotewrite]
  telemetry:
    logs:
      level: debug
