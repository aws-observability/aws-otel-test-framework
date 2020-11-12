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

variable "region" {
  default = "us-west-2"
}

variable "testing_ami" {
  default = "amazonlinux2"
}

variable "soaking_data_emitter_image" {
  default = "aottestbed/aws-otel-load-generator:v0.1.0"
}

# set this option to false will disable validator to call the sample app
# in some cases, it's needed, for example, ecsmetric receiver collect metric automatically even without data emitter
variable "sample_app_callable" {
  default = false
}

variable "soaking_metric_namespace" {
  default = "AWSOtelCollector/SoakingTest"
}

variable "testcase" {
  default = "../testcases/otlp"
}

# data type will be emitted. Possible values: metric or trace
variable "date_mode" {
  default = "metric"
}

# data points were emitted per second
variable "rate" {
  default = 1000
}

# data model type. possible values: otlp, xray, etc
variable "data_type" {
  default = "otlp"
}

variable "validation_config" {
  default = "default-validation.yml"
}

# ec2 host instance type for running aws-otel-collector
variable "instance_type_for_collector" {
  default = "m5.2xlarge"
}

# ec2 host instance type for running load generator
variable "instance_type_for_emitter" {
  default = "t2.micro"
}




