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

module "common" {
  source = "../common"

  data_emitter_image = var.data_emitter_image
  aoc_version = var.aoc_version
}

module "basic_components" {
  source = "../basic_components"

  region = var.region

  testcase = var.testcase

  testing_id = module.common.testing_id

}

module "ec2_setup" {
  source = "../ec2"

  region = var.region

  data_emitter_image = var.soaking_data_emitter_image
  testcase = var.testcase
  aoc_version = var.aoc_version
  sshkey_s3_bucket = var.sshkey_s3_bucket
  validation_config = var.validation_config
  enable_alarming = true

  # soaking test config
  soaking_compose_file = "../template/defaults/soaking_docker_compose.tpl"
  date_mode = var.date_mode
  rate = var.rate
  data_type = var.data_type
  # ec2 instance type
  instance_type_for_emitter = var.instance_type_for_emitter
  instance_type_for_collector = var.instance_type_for_collector
}

provider "aws" {
  region  = var.region
}
