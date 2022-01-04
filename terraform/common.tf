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

variable "mocked_server" {
  default = "https"
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
  default = "latest"
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
  type    = bool
  default = false
}

variable "soaking_data_mode" {
  default = "metric"
}

variable "soaking_data_type" {
  default = "otlp"
}

variable "sample_app_mode" {
  default = "push"
}

variable "cortex_instance_endpoint" {
  # change to your cortex endpoint
  default = "https://aps-workspaces.us-west-2.amazonaws.com/workspaces/ws-cd278045-1d6e-4550-a207-c5046a1b40b8"
}

variable "aotutil" {
  default = "../../cmd/aotutil/aotutil"
}

variable "aoc_vpc_name" {
  default = "aoc-vpc"
}

variable "aoc_vpc_security_group" {
  default = "aoc-vpc-security-group"
}