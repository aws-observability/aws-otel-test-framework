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
    translation_rules:
    - action: drop_dimensions
      metric_names:
        http.server.duration_count: true
        http.server.duration_sum: true
        http.server.duration_min: true
        http.server.duration_max: true
        http.server.duration_bucket: true
        process.runtime.jvm.memory.usage: true
      dimension_pairs:
        "aws.ecs.container.arn":
        "aws.ecs.container.image.id":
        "aws.ecs.launchtype":
        "aws.ecs.task.arn":
        "aws.ecs.task.family":
        "aws.ecs.task.revision":
        "aws.log.group.arns":
        "aws.log.group.names":
        "aws.log.stream.arns":
        "aws.log.stream.names":
        "cloud.account.id":
        "cloud.availability_zone":
        "cloud.platform":
        "cloud.provider":
        "cloud.region":

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
