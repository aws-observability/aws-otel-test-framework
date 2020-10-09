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

variable "eks_pod_config_path" {
  default = "../template/eks-pod-config/default-eks-config.tpl"
}

# set this option to false will disable validator to call the sample app
# in some cases, it's needed, for example, ecsmetric receiver collect metric automatically even without data emitter
variable "sample_app_callable" {
  default = true
}
