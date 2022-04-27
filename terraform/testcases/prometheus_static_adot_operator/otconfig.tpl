extensions:
  sigv4auth:
    region: ${region}
    service: "aps"
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
  prometheusremotewrite:
    endpoint: ${cortex_instance_endpoint}/api/v1/remote_write
    timeout: 15s
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
      level: debug
