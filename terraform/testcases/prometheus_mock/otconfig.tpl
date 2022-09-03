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
  prometheusremotewrite:
    endpoint: "https://${mock_endpoint}"
    auth:
      authenticator: sigv4auth
service:
  pipelines:
    metrics:
     receivers: [prometheus]
     exporters: [prometheusremotewrite]
  extensions: [sigv4auth]
  telemetry:
    logs:
      level: ${log_level}
