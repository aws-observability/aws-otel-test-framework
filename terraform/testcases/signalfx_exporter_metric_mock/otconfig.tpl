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
  signalfx:
    access_token: dummytoken
    ingest_url: "https://${mock_endpoint}"
    api_url: "http://localhost/dummy_url"

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [signalfx]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
