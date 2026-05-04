extensions:
  pprof:
    endpoint: 0.0.0.0:1777
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}
  kafka/receiver:
    protocol_version: "${extra_data.msk.kafka_version}"
    auth:
      tls:
        insecure: false
    brokers:
%{ for broker in split(",", extra_data["msk"].bootstrap_brokers_tls) }      - ${broker}
%{ endfor }
    traces:
      topics:
        - ${extra_data.msk.topic}

processors:
  batch:

exporters:
  awsxray:
    local_mode: true
    region: '${region}'
  kafka/exporter:
    protocol_version: "${extra_data.msk.kafka_version}"
    auth:
      tls:
        insecure: false
    brokers:
%{ for broker in split(",", extra_data["msk"].bootstrap_brokers_tls) }      - ${broker}
%{ endfor }
    traces:
      topic: ${extra_data.msk.topic}

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [kafka/exporter]
    traces/2:
      receivers: [kafka/receiver]
      processors: [batch]
      exporters: [awsxray]
  extensions: [pprof]
  telemetry:
    logs:
      level: ${log_level}
