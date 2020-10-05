variable "otconfig_path" {
  default = "../template/otconfig/default_otconfig.tpl"
}

variable "package_s3_bucket" {
  default = "aws-observability-collector-test"
}

variable "aoc_version" {
  default = "v0.1.11"
}

variable "region" {
  default = "us-west-2"
}

variable "testing_ami" {
  default = "amazonlinux2"
}