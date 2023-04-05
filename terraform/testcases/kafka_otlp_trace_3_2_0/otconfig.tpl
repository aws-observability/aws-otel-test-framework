extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}
  kafka/receiver:
    topic: ${extra_data.msk.topic}
    protocol_version: "${extra_data.msk.kafka_version}"
    auth:
      tls:
        insecure: false
    brokers:
%{ for broker in split(",", extra_data["msk"].bootstrap_brokers_tls) }      - ${broker}
%{ endfor }

processors:
  batch:

exporters:
  logging:
    verbosity: detailed
  awsxray:
    local_mode: true
    region: '${region}'
  kafka/exporter:
    protocol_version: "${extra_data.msk.kafka_version}"
    auth:
      tls:
        insecure: false
    topic: ${extra_data.msk.topic}
    brokers:
%{ for broker in split(",", extra_data["msk"].bootstrap_brokers_tls) }      - ${broker}
%{ endfor }

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [logging,kafka/exporter]
    traces/2:
      receivers: [kafka/receiver]
      processors: [batch]
      exporters: [logging,awsxray]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
