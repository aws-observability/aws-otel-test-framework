validation_config = "spark-otel-trace-validation.yml"

# data type will be emitted. Possible values: metric or trace
soaking_data_mode = "trace"

eks_cluster_name = "adot-op-cluster"

sample_app = "spark"

sample_app_image = "public.ecr.aws/aws-otel-test/aws-otel-java-spark:latest"
