otconfig_path="../template/otconfig/xrayreceiver_otconfig.tpl"
ecs_taskdef_path="../template/ecstaskdef/xrayreceiver_ecs_taskdef.tpl"

# this file is defined in validator/src/main/resources/validations
validation_config="xrayreceiver-trace-validation.yml"

data_emitter_image="josephwy/integ-test-emitter:xrayreceiver"

# todo this is config needs to be removed once we have statsd built in the aoc image
aoc_image_repo="josephwy/awscollector"

# todo this version needs to be removed, instead version should be received from workflow
aoc_version="xray"

