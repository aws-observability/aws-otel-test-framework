validation_config = "default-mocked-server-xrayreceiver-validation.yml"

# data type will be emitted. Possible values: metric or trace
soaking_data_mode = "trace"

# data model type. possible values: otlp, xray, etc
soaking_data_type = "xray"

sample_app_image = "public.ecr.aws/aws-otel-test/aws-otel-goxray-sample-app:v1.3.0"
