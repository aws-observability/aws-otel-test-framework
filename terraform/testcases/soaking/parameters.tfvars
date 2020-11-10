# enable soaking
soaking = true

# assuming the sample app will generate high volume data by default
sample_app_callable = false

# ask validator to pull the alarms
validation_config = "alarm-pulling-validation.yml"

# use amazonlinux2 by default to soak
testing_ami = "amazonlinux2"

data_emitter_image = "mxiamxia/aws-otel-load-generator:v0.1.7"



