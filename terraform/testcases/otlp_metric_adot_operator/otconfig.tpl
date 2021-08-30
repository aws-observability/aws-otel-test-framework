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
      logging:
        loglevel: debug
      awsemf:
        region: '${region}'

    service:
      pipelines:
        metrics:
          receivers: [otlp]
          processors: [batch]
          exporters: [logging,awsemf]
      extensions: [pprof]