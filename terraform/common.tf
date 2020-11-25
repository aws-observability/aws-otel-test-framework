variable "region" {
  default = "us-west-2"
}

variable "validation_config" {
  default = "default-mocked-server-validation.yml"
}

variable "testcase" {
  default = "../testcases/otlp_mock"
}

variable "sample_app" {
  default = "spark"
}

## mocked server related
# we use mocked_server_image if it's not empty, if it's empty, the image will come from the basic component, which is built by imagebuild module
variable "mocked_server_image" {
  default = ""
}

# we use sample_app_image if it's not empty, if it's empty, the sample_app_image will come from the basic component, which is built by imagebuild module
# instead "sample_app" will be used to choose the image
variable "sample_app_image" {
  default = ""
}

variable "aoc_image_repo" {
  default = "611364707713.dkr.ecr.us-west-2.amazonaws.com/aws/aws-otel-collector"
}

variable "aoc_version" {
  default = "v0.4.0-382067259"
}

variable "aws_access_key_id" {
  default = ""
}

variable "aws_secret_access_key" {
  default = ""
}

variable "soaking_metric_namespace" {
  default = "AWSOtelCollector/SoakingTest"
}

variable "debug" {
  type = bool
  default = false
}

variable "soaking_data_mode" {
  default = "metric"
}
