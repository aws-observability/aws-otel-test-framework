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

provider "aws" {
  region = var.region
}

module "common" {
  source = "../common"
}

module "basic_components" {
  source          = "../basic_components"
  region          = var.region
  testcase        = var.testcase
  testing_id      = module.common.testing_id
  mocked_endpoint = var.mock_endpoint
  sample_app      = var.sample_app
  mocked_server   = var.mocked_server
  debug           = var.debug
}

# launch ec2
module "ec2_setup" {
  source = "../ec2"

  ami_family        = var.ami_family
  amis              = var.amis
  testing_ami       = var.testing_ami
  aoc_version       = var.aoc_version
  region            = var.region
  testcase          = var.testcase
  validation_config = var.validation_config
  mock_endpoint     = var.mock_endpoint
  mocked_server     = var.mocked_server
  sample_app_image  = var.sample_app_image != "" ? var.sample_app_image : module.basic_components.sample_app_image
  package_s3_bucket = "aws-otel-collector"
  skip_validation   = false
  canary            = true

  # install cwagent
  install_cwagent = false

  patch = true

  ssm_package_name       = "AWSDistroOTel-Collector"
  install_package_source = var.install_package_source
  ssm_config             = var.ssm_config
  disable_mocked_server  = var.disable_mocked_server
  enable_ssm_validate    = var.enable_ssm_validate
}