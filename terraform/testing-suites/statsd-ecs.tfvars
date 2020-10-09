sample_app_callable = false

otconfig_path="../template/otconfig/statsd_otconfig.tpl"
ecs_taskdef_path="../template/ecstaskdef/statsd_taskdef.tpl"

# this file is defined in validator/src/main/resources/validations
validation_config="statsd-metric-validation.yml"

data_emitter_image="alpine/socat:latest"

# todo this is config needs to be removed once we have statsd built in the aoc image
aoc_image_repo="josephwy/awscollector"

# todo this version needs to be removed, instead version should be received from workflow
aoc_version="v0.1.13"