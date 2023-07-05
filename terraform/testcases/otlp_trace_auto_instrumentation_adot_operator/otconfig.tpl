receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:${grpc_port}

    processors:
      batch:

    exporters:
      logging:
        verbosity: detailed
      awsxray:
        local_mode: true
        region: '${region}'

    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [batch]
          exporters: [logging,awsxray]
      telemetry:
        logs:
          level: ${log_level}
