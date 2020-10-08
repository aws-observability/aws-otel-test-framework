variable "otconfig_path" {
  default = "../template/otconfig/default_otconfig.tpl"
}

variable "docker_compose_path" {
  default = "../template/ec2-docker-compose-config/default_ec2_docker_compose.yml"
}

variable "package_s3_bucket" {
  default = "aws-observability-collector-test"
}

variable "aoc_version" {
  default = "v0.1.12-291936394"
}

variable "region" {
  default = "us-west-2"
}

variable "testing_ami" {
  default = "amazonlinux2"
}

variable "validation_config" {
  default = "default-validation.yml"
}

variable "data_emitter_image" {
  default = "josephwy/integ-test-emitter:alpine"
}

variable "data_emitter_image_command" {
  default = ""
}

variable "sshkey_s3_bucket" {
  default = "aoc-ssh-key"
}

variable "sshkey_s3_private_key" {
  default = "aoc-ssh-key-2020-07-22.txt"
}

# set this option to false will disable validator to call the sample app
# in some cases, it's needed, for example, ecsmetric receiver collect metric automatically even without data emitter
variable "sample_app_callable" {
  default = true
}
