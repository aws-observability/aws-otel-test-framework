# this file is defined in validator/src/main/resources/validations
validation_config = "otlp-prometheus-validation.yml"

# data type will be emitted. Possible values: metric or trace
soaking_data_mode = "metric"

sample_app = "spark"

sample_app_image = "public.ecr.aws/aws-otel-test/aws-otel-java-spark:latest"
