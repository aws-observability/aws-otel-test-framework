receivers:
  otlp/loadbalancer:
    protocols:
      grpc:
        endpoint: 0.0.0.0:${grpc_port}
  otlp/backend-1:
    protocols:
      grpc:
        endpoint: localhost:55690
  otlp/backend-2:
    protocols:
      grpc:
        endpoint: localhost:55700
  otlp/backend-3:
    protocols:
      grpc:
        endpoint: localhost:55710
  otlp/backend-4:
    protocols:
      grpc:
        endpoint: localhost:55720

processors:
  attributes/backend-1:
    actions:
      - key: collector-id
        value: 1
        action: insert
  attributes/backend-2:
    actions:
      - key: collector-id
        value: 2
        action: insert
  attributes/backend-3:
    actions:
      - key: collector-id
        value: 3
        action: insert
  attributes/backend-4:
    actions:
      - key: collector-id
        value: 4
        action: insert
  batch:
    

exporters:
  debug:
  loadbalancing:
    protocol:
      otlp:
        tls:
          insecure: true
    resolver:
      static:
        hostnames:
        - localhost:55690
        - localhost:55700
        - localhost:55710
        - localhost:55720
  awsxray:
    local_mode: true
    region: '${region}'

service:
  pipelines:
    traces/loadbalancer:
      receivers:
        - otlp/loadbalancer
      processors: [batch]
      exporters:
        - loadbalancing

    traces/backend-1:
      receivers:
        - otlp/backend-1
      processors: [attributes/backend-1]
      exporters:
        - awsxray

    traces/backend-2:
      receivers:
        - otlp/backend-2
      processors: [attributes/backend-2]
      exporters:
        - awsxray

    traces/backend-3:
      receivers:
        - otlp/backend-3
      processors: [attributes/backend-3]
      exporters:
        - awsxray

    traces/backend-4:
      receivers:
        - otlp/backend-4
      processors: [attributes/backend-4]
      exporters:
        - awsxray
  telemetry:
    logs:
      level: ${log_level}