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
  coralogix:
    endpoint: "${mock_endpoint}"
    tls:
      insecure: true
    metrics:
      endpoint: "${mock_endpoint}"
    private_key: "e83138ae-fe21-11ec-b939-0242ac120002"
    application_name: "APP_NAME"
    subsystem_name: "SUBSYSTEM"
service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [coralogix]
  extensions: [pprof]
  telemetry:
    logs:
      level: debug
