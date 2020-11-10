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

variable "validation_config" {
  default = "default-mocked-server-validation.yml"
}

variable "data_emitter_image" {
  default = "josephwy/integ-test-emitter:alpine"
}

# set this option to false will disable validator to call the sample app
# in some cases, it's needed, for example, ecsmetric receiver collect metric automatically even without data emitter
variable "sample_app_callable" {
  default = true
}

variable "testcase" {
  default = "../testcases/otlp_mock"
}

variable "collector_repo_path" {
  default = "../../../aws-otel-collector"
}


