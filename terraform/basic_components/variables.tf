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

variable "aoc_vpc_name" {
  default = "aoc-vpc"
}

variable "aoc_vpc_security_group" {
  default = "aoc-vpc-security-group"
}

variable "region" {
  default = "us-west-2"
}

variable "testcase" {
}

variable "testing_id" {
}

variable "mocked_endpoint" {
}

variable "sample_app" {
}

variable "mocked_server" {
}

variable "cortex_instance_endpoint" {
  default = ""
}

variable "sample_app_listen_address_host" {
  default = ""
}

variable "sample_app_listen_address_port" {
  default = ""
}

variable "debug" {
  type    = bool
  default = false
}

variable "extra_data" {
  type = map
  default = {}
}
