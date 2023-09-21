# this file is defined in validator/src/main/resources/validations
validation_config = "statsd-otellib-metric-validation.yml"

# sample application image to emit the trace data
sample_app = "statsd"

# data type will be emitted. Possible values: metric or trace
soaking_data_mode = "metric"

# data model type. possible values: otlp, xray, etc
soaking_data_type = "statsd"

feature_gate = "+aws.statsd.populateInstrumentationScope"

aoc_image_repo = "public.ecr.aws/aws-otel-test/adot-collector-integration-test"

aoc_version = "v0.30.0-9468cd0"

