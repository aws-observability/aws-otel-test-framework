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
  default = "josephwy/integ-test-emitter:ying"
}

variable "aoc_image_repo" {
  default = "josephwy/ttt"
}

variable "aoc_version" {
  default = "v0.1.11"
}

variable "validation_config" {
  default = "default-validation.yml"
}

