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

variable "region" {
  default = "us-west-2"
}

variable "security_group_name" {
  default = "aoc-vpc-security-group"
}

variable "aoc_iam_role" {
  default = "aoc-e2e-iam-role"
}

variable "otel_service_namespace" {
  default = "YingOtel"
}

variable "otel_service_name" {
  default = "Terraform"
}

variable "aoc_vpc_name" {
  default = "aoc-vpc"
}

variable "aoc_image_repo" {
  default = "josephwy/ttt"
}

variable "aoc_version" {
  default = "v0.1.11"
}