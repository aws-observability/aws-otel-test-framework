extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}
  kafka/receiver:
    protocol_version: "2.8.1"
    auth:
      tls:
        insecure: false
    brokers:
%{ for broker in split(",", extra_data["msk"].bootstrap_brokers_tls) }      - ${broker}
%{ endfor }
    metrics:
      topics:
        - ${extra_data.msk.topic}

processors:
  batch:

exporters:
  awsemf:
    region: '${region}'
  kafka/exporter:
    protocol_version: "2.8.1"
    auth:
      tls:
        insecure: false
    brokers:
%{ for broker in split(",", extra_data["msk"].bootstrap_brokers_tls) }      - ${broker}
%{ endfor }
    metrics:
      topic: ${extra_data.msk.topic}

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [kafka/exporter]
    metrics/2:
      receivers: [kafka/receiver]
      exporters: [awsemf]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
