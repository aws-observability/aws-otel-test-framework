# this file is defined in validator/src/main/resources/validations
validation_config = "default-xray-trace-validation.yml"

# sample application image to emit the trace data
# src: https://github.com/aws/aws-xray-sdk-go/blob/master/sample-apps/http-server/application.go
sample_app_image = "public.ecr.aws/aws-otel-test/aws-otel-goxray-sample-app:v1.3.0"

# data type will be emitted. Possible values: metric or trace
soaking_data_mode = "trace"

# data model type. possible values: otlp, xray, etc
soaking_data_type = "xray"