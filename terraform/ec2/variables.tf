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

## right now there's no good way to share variables across modules,
## so we have to define some of the common vars like region, otconfig_path in each module

variable "package_s3_bucket" {
  default = "aws-otel-collector-test"
}

variable "aoc_version" {
  default = "v0.3.0-346703560"
}

variable "region" {
  default = "us-west-2"
}

variable "testing_ami" {
  default = "amazonlinux2"
}

variable "validation_config" {
  default = "default-mocked-server-validation.yml"
}

variable "data_emitter_image" {
  default = "josephwy/integ-test-emitter:new"
}

variable "data_emitter_image_command" {
  default = ""
}

variable "sshkey_s3_bucket" {
  default = "aoc-ssh-key"
}

variable "sshkey_s3_private_key" {
  default = "aoc-ssh-key-2020-07-22.pem"
}

# create soaking alarm if this flag is on
variable "soaking" {
  default = false
}

variable "soaking_metric_namespace" {
  default = "AWSOtelCollector/SoakTest"
}

variable "testcase" {
  default = "../testcases/otlp_mock"
}

## mocked server related
variable "mocked_server_image" {
  default = "josephwy/mocked-server:v0.1.3"
}
