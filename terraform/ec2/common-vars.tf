variable "trace_data_bucket" {
  default = "trace-expected-data"
}

variable "otel_service_namespace" {
  default = "YingOtel"
}

variable "otel_service_name" {
  default = "Terraform"
}

variable "aoc_version" {
  default = "latest"
}

variable "aoc_image" {
  default = "josephwy/integ-test-emitter"
}

