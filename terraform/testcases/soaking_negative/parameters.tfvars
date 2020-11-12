# assuming the sample app will generate high volume data by default
sample_app_callable = false

# ask validator to pull the alarms
validation_config = "alarm-pulling-validation.yml"

# data type will be emitted. Possible values: metric or trace
date_mode = "trace"

# data points were emitted per second
rate = "1000"

# data model type. possible values: otlp, xray, etc
data_type = "otlp"


