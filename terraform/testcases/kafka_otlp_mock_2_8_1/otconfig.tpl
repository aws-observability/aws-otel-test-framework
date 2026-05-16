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
    traces:
      topics:
        - ${extra_data.msk.topic}

processors:
  batch:

exporters:
  awsxray:
    region: '${region}'
    local_mode: true
    no_verify_ssl: false
    endpoint: "${mock_endpoint}"
  kafka/exporter:
    protocol_version: "2.8.1"
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
