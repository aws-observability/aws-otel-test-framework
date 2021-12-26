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
  type = string
  default = "us-west-2"
}

variable "testing_id" {
  type = string
  default = ""
  description = "Test case's unique id"
}

variable "testcase" {
  type = string
  default = "../../testcases/otlp_mock"
  description = "Test case that runs with the platform"
}

variable "platform" {
  type = string
  default = ""
  description = "Platform to upload terraform state. Need to be in one of the platforms: ec2, eks, ecs, soaking, canary"
}

variable "folder_name" {
  type = string
  default = "dummy_test"
  description = "Folder name when uploading to s3"
}