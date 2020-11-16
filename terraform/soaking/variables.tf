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
variable "soaking_data_emitter_image" {
  default = "aottestbed/aws-otel-load-generator:v0.1.0"
}

# data type will be emitted. Possible values: metric or trace
variable "soaking_data_mode" {
  default = "metric"
}

# data points were emitted per second
variable "soaking_data_rate" {
  default = 1000
}

# data model type. possible values: otlp, xray, etc
variable "soaking_data_type" {
  default = "otlp"
}

variable "testing_ami" {
  default = "soaking_linux"
}

variable "negative_soaking" {
  default = false
}
