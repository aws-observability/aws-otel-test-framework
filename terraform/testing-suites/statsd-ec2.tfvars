sample_app_callable = false

otconfig_path="../template/otconfig/statsd_otconfig.tpl"

# todo create a docker compose file and replace this path
docker_compose_path="../template/ec2-docker-compose-config"

# todo create a s3 bucket to store rpm, and put its name here
package_s3_bucket=""

# this file is defined in validator/src/main/resources/validations
validation_config="statsd-metric-validation.yml"

# todo this is config needs to be removed once we have statsd built in the aoc image
aoc_image_repo="gavindoudou/aocrepo"

# todo this version needs to be removed, instead version should be received from workflow
aoc_version="v0.1.10"

data_emitter_image="alpine/socat:latest"