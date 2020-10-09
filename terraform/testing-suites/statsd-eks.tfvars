sample_app_callable = false

otconfig_path="../template/otconfig/statsd_otconfig.tpl"
eks_pod_config_path="../template/eks-pod-config/statsd-eks-config.tpl"

# this file is defined in validator/src/main/resources/validations
validation_config="statsd-metric-validation.yml"

data_emitter_image="alpine/socat:latest"

# todo this is config needs to be removed once we have statsd built in the aoc image
aoc_image_repo="gavindoudou/aocrepo"

# todo this version needs to be removed, instead version should be received from workflow
aoc_version="v0.1.10"