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
  default = "v0.1.0-324996423"
}

variable "region" {
  default = "us-west-2"
}

variable "testing_ami" {
  default = "amazonlinux2"
}

variable "validation_config" {
  default = "default-validation.yml"
}

variable "data_emitter_image" {
  default = "josephwy/integ-test-emitter:alpine"
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

# set this option to false will disable validator to call the sample app
# in some cases, it's needed, for example, ecsmetric receiver collect metric automatically even without data emitter
variable "sample_app_callable" {
  default = true
}

# create soaking alarm if this flag is on
variable "soaking" {
  default = false
}

variable "soaking_metric_namespace" {
  default = "AWSOtelCollector/SoakTest"
}

variable "testcase" {
  default = "../testcases/otlp"
}

