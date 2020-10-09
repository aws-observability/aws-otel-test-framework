variable "region" {
  default = "us-west-2"
}

variable "otconfig_path" {
  default = "../template/otconfig/default_otconfig.tpl"
}

variable "ecs_taskdef_path" {
  default = "../template/ecstaskdef/default_ecs_taskdef.tpl"
}

variable "ecs_launch_type" {
  default = "EC2"
}

variable "data_emitter_image" {
  default = "josephwy/integ-test-emitter:alpine"
}

variable "aoc_image_repo" {
  default = "611364707713.dkr.ecr.us-west-2.amazonaws.com/aws/aws-observability-collector"
}

variable "aoc_version" {
  default = "v0.1.12-296689894"
}

variable "validation_config" {
  default = "default-validation.yml"
}

# set this option to false will disable validator to call the sample app
# in some cases, it's needed, for example, ecsmetric receiver collect metric automatically even without data emitter
variable "sample_app_callable" {
  default = true
}

