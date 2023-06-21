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
      awsemf:
        region: '${region}'

    service:
      pipelines:
        metrics:
          receivers: [otlp]
          processors: [ ]
          exporters: [logging,awsemf]
      telemetry:
        logs:
          level: ${log_level}
