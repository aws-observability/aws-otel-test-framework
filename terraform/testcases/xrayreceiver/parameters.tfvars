# this file is defined in validator/src/main/resources/validations
validation_config="default-xray-trace-validation.yml"

# sample application image to emit the trace data
data_emitter_image="611364707713.dkr.ecr.us-west-2.amazonaws.com/amazon/aws-otel-goxray-sample-app:v1.1.0"

soaking_data_emitter_image="aottestbed/aws-otel-load-generator:v0.1.0"

# data type will be emitted. Possible values: metric or trace
date_mode = "trace"

# data model type. possible values: otlp, xray, etc
data_type = "xray"