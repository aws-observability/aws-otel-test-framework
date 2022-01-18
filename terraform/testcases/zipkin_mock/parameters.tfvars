validation_config = "zipkin-mocked-server-validation.yml"

sample_app_image = "public.ecr.aws/aws-otel-test/jaeger-zipkin-sample-app:v0.1.5"

sample_app = "jaeger-zipkin-sample-app"

# data type will be emitted. Possible values: metric or trace
soaking_data_mode = "trace"

# data model type. possible values: otlp, xray, etc
soaking_data_type = "zipkin"
