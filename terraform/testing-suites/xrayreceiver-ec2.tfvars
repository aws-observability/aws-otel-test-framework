otconfig_path="../template/otconfig/xrayreceiver_otconfig.tpl"

# this file is defined in validator/src/main/resources/validations
validation_config="xray-receiver-trace-validation.yml"

# sample application image to emit the trace data
data_emitter_image="johnwu20/samplesever:v2.1.0"

# AOC image
aoc_image_repo="johnwu20/aocimage"

# todo this version needs to be removed, instead version should be received from workflow
aoc_version="v0.1.1"

region="us-west-2"