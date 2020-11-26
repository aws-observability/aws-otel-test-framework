extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}

processors:
  batch/metrics:
    timeout: 60s

exporters:
  logging:
    loglevel: debug
  signalfx:
    access_token: dummytoken
    ingest_url: "https://${mock_endpoint}"
    api_url: "http://localhost/dummy_url"

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch/metrics]
      exporters: [signalfx]
  extensions: [pprof]
