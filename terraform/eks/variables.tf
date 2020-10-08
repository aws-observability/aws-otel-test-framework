variable "eks_cluster_name" {
  default = "aoc-test-eks-ec2"
}

variable "otconfig_path" {
  default = "../template/otconfig/default_otconfig.tpl"
}

variable "region" {
  default = "us-west-2"
}

variable "data_emitter_image" {
  default = "josephwy/integ-test-emitter:alpine"
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
