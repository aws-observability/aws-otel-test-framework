extensions:
  sigv4auth:
    region: ${region}
    service: "aps"
receivers:
      prometheus:
        config:
          scrape_configs:
          - job_name: 'kubernetes-service-endpoints'
            kubernetes_sd_configs:
            - role: endpoints
            relabel_configs:
            - source_labels: [ __meta_kubernetes_namespace ]
              action: keep
              regex: "aoc-ns-${testing_id}"
            - source_labels: [ __meta_kubernetes_pod_label_app ]
              action: keep
              regex: "sample-app"
    exporters:
      awsprometheusremotewrite:
        endpoint: ${cortex_instance_endpoint}/api/v1/remote_write
        aws_auth:
          region: ${region}
          service: "aps"
        timeout: 10s
      prometheusremotewrite:
        endpoint: ${cortex_instance_endpoint}/api/v1/remote_write
        timeout: 10s
        auth:
          authenticator: sigv4auth
      logging:
        loglevel: debug
    service:
      pipelines:
        metrics:
          receivers: [prometheus]
          exporters: [awsprometheusremotewrite, prometheusremotewrite, logging]
      extensions: [sigv4auth]
      telemetry:
        logs:
          level: debug
