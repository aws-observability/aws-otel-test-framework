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

variable "validation_config" {
  default = "default-mocked-server-validation.yml"
}

variable "testing_id" {}

variable "region" {
}

variable "sample_app_endpoint" {
  default = ""
}

variable "mocked_server_validating_url" {
  default = ""
}

variable "metric_namespace" {
  default = ""
}

variable "ecs_cluster_name" {
  default = ""
}

variable "ecs_task_arn" {
  default = ""
}

variable "ecs_taskdef_family" {
  default = ""
}

variable "ecs_taskdef_version" {
  default = ""
}

variable "cpu_alarm" {
  default = ""
}

variable "mem_alarm" {
  default = ""
}

variable "aws_access_key_id" {
  default = ""
}

variable "aws_secret_access_key" {
  default = ""
}