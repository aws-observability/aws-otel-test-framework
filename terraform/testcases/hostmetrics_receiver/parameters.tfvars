# this file is defined in validator/src/main/resources/validations
validation_config     = "hostmetrics-metric-linux-validation.yml" #incase windows ami_family is used in ec2 then this property will be overridden in EC2
sample_app_callable   = false
disable_mocked_server = true
scheduling_strategy   = "DAEMON"
rollup                = false #config has NoDimensionRollup policy
ecs_launch_type       = "EC2"
