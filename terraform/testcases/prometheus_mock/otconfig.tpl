extensions:
  sigv4auth:
    region: "us-west-2"
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
  prometheusremotewrite:
    endpoint: "https://${mock_endpoint}"
service:
  pipelines:
    metrics:
     receivers: [prometheus]
     exporters: [awsprometheusremotewrite,prometheusremotewrite]
  extensions: [sigv4auth]
  telemetry:
    logs:
      level: debug
