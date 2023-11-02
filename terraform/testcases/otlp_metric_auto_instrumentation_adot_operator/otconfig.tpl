receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:${grpc_port}

    processors:
      batch:

    exporters:
      awsemf:
        region: '${region}'

    service:
      pipelines:
        metrics:
          receivers: [otlp]
          processors: [batch]
          exporters: [awsemf]
      telemetry:
        logs:
          level: ${log_level}
