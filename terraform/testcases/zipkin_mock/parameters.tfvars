validation_config="zipkin-mocked-server-validation.yml"

sample_app_image="aottestbed/aws-otel-jz-sample-app:v0.1.4"

sample_app="aws-otel-jz-sample-app"

# data type will be emitted. Possible values: metric or trace
soaking_data_mode = "trace"

# data model type. possible values: otlp, xray, etc
soaking_data_type = "zipkin"
