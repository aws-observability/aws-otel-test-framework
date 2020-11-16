# this file is defined in validator/src/main/resources/validations
validation_config="default-xray-trace-validation.yml"

# sample application image to emit the trace data
sample_app_image="611364707713.dkr.ecr.us-west-2.amazonaws.com/amazon/aws-otel-goxray-sample-app:v1.1.0"

# data type will be emitted. Possible values: metric or trace
soaking_data_mode = "trace"

# data model type. possible values: otlp, xray, etc
soaking_data_type = "xray"