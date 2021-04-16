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

variable "testing_id" {
  type = string
}

variable "eks_pod_config_path" {
  type = string
}

variable "sample_app" {
  type = object({
    image               = string
    name                = string
    metric_namespace    = string
    mode                = string
    listen_address_ip   = string
    listen_address_port = string
  })
}

variable "aoc_namespace" {}

variable "aoc_service" {
  type = object({
    name      = string
    grpc_port = string
    udp_port  = string
    http_port = string
  })
}