# this file is defined in validator/src/main/resources/validations
validation_config = "statsd-metric-validation.yml"

# sample application image to emit the trace data
sample_app = "statsd"

# data type will be emitted. Possible values: metric or trace
soaking_data_mode = "metric"

# data model type. possible values: otlp, xray, etc
soaking_data_type = "statsd"
