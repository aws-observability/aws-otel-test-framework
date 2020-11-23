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

module "ec2_setup" {
  source = "../ec2_setup"

  testing_ami = var.testing_ami
  soaking_data_rate = var.data_rate
  soaking_data_type = var.data_type
  install_package_source = var.install_package_source
  install_package_local_path = var.install_package_local_path
}
