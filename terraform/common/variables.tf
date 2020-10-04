variable "region" {
  default = "us-west-2"
}

variable "security_group_name" {
  default = "aoc-test-security-group"
}

variable "data_emitter_image" {
  default = "josephwy/integ-test-emitter:0.9.1"
}

variable "aoc_image_repo" {
  default = "josephwy/ttt"
}

variable "aoc_version" {
  default = "v0.1.11"
}

variable "aoc_iam_role" {
  default = "aoc-e2e-iam-role"
}

variable "otel_service_namespace" {
  default = "YingOtel"
}

variable "otel_service_name" {
  default = "Terraform"
}