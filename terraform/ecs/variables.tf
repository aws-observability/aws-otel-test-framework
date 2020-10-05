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