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
  region  = var.region
}

module "common" {
  source = "../common"
}

module "basic_components" {
  source = "../basic_components"
  region = var.region
  testcase = var.testcase
  testing_id = module.common.testing_id
  mocked_endpoint = var.mock_endpoint
  sample_app = var.sample_app
}

# launch ec2
module "ec2_setup" {
  source = "../ec2"

  ami_family = var.ami_family
  amis = var.amis
  testing_ami = var.testing_ami
  aoc_version = var.aoc_version
  region = var.region
  canary = var.canary
  testcase = var.testcase
  validation_config = var.validation_config
  sample_app_image = var.sample_app_image != "" ? var.sample_app_image : module.basic_components.sample_app_image
  skip_validation = false

  # install cwagent
  install_cwagent = false

  aws_access_key_id = var.aws_access_key_id
  aws_secret_access_key = var.aws_secret_access_key
}