extensions:
      pprof:
        endpoint: 0.0.0.0:1777
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:${grpc_port}

    processors:
      batch:

    exporters:
      awsxray:
        local_mode: true
        region: '${region}'

    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [batch]
          exporters: [awsxray]
      extensions: [pprof]
      telemetry:
        logs:
          level: ${log_level}
