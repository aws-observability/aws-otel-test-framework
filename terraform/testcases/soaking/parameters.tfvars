# enable soaking
soaking = true

# assuming the sample app will generate high volume data by default
sample_app_callable = false

# ask validator to pull the alarms
validation_config = "alarm-pulling-validation.yml"

# use amazonlinux2 by default to soak
testing_ami = "amazonlinux2"

# load test emitter
data_emitter_image = "aottestbed/aws-otel-load-generator:v0.1.0"

# EC2 instance type for running aws-otel-collector
instance_type_for_collector = "m5.2xlarge"

# EC2 instance type for running load generator
instance_type_for_emitter = "m5.2xlarge"



