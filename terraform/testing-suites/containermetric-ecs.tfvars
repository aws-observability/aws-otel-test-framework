otconfig_path="../template/otconfig/ecscontainermetric_otconfig.tpl"
ecs_taskdef_path="../template/ecstaskdef/ecscontainermetric_taskdef.tpl"

# this file is defined in validator/src/main/resources/validations
validation_config="ecscontainer-metric-validation.yml"

# set data emitter image to empty, which helps the framework to skip deploying data emitter
data_emitter_image=""

sample_app_callable = false
