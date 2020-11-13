# ------------------------------------------------------------------------
# Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
# -------------------------------------------------------------------------

variable "eks_cluster_name" {
  default = "aoc-test-eks-ec2"
}

variable "region" {
  default = "us-west-2"
}

variable "aoc_image_repo" {
  default = "611364707713.dkr.ecr.us-west-2.amazonaws.com/aws/aws-otel-collector"
}

variable "aoc_version" {
  default = "v0.3.0-346703560"
}

variable "validation_config" {
  default = "default-mocked-server-validation.yml"
}

variable "testcase" {
  default = "../testcases/otlp_mock"
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

variable "sample_app" {
  default = "spark"
}

variable "aws_access_key_id" {
  default = ""
}

variable "aws_secret_access_key" {
  default = ""
}
