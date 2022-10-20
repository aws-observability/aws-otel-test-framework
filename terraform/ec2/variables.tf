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

## right now there's no good way to share variables across modules,
## so we have to define some of the common vars like region, otconfig_path in each module

variable "package_s3_bucket" {
  default = "aws-otel-collector-test"
}

variable "testing_ami" {
  default = "amazonlinux2"
}

variable "sidecar_instance_type" {
  default = "t3.medium"
}

variable "soaking_compose_file" {
  default = ""
}

######################
# Soaking related
######################
# data points were emitted per second
variable "soaking_data_rate" {
  default = 100
}

variable "skip_validation" {
  default = false
}

variable "mock_endpoint" {
  default = "mocked-server/put-data"
}

variable "install_cwagent" {
  default = false
}

# if ssh_key_name is empty, we create private key every time we create instance.
# if not, we pull the private key from s3.
variable "ssh_key_name" {
  default = ""
}

variable "sshkey_s3_bucket" {
  default = ""
}

variable "sshkey_s3_private_key" {
  default = ""
}

variable "commit_id" {
  default = ""
}

variable "launch_date" {
  default = ""
}

variable "negative_soaking" {
  default = false
}


# options: s3, local, ssm
variable "install_package_source" {
  default = "s3" # which means we download rpm/dev/msi from s3, the links are defined in the ami map.
}

# use this parameter when install_package_source is local
variable "install_package_local_path" {
  default = "../../../aws-otel-collector/build/packages/linux/amd64/aws-otel-collector.rpm"
}

variable "testing_type" {
  default = "e2e"
}

# flag used by canary test to run EC2 tests in canary mode
variable "canary" {
  default = false
}

# if patch is true, we will wait until the instance gets patched after launching the instance,
# also set a patch tag onto the instance so that the instance get picked by the ssm patching process.
# and then start the installation of collector.
variable "patch" {
  default = true
}

######################
# SSM related
######################

# ADOT SSM package name
variable "ssm_package_name" {
  default = "AWSDistroOTel-Collector"
}

# Base64 value of ADOT Collector YAML configuration
variable "ssm_config" {
  default = ""
}

# bypass mocked server setup
variable "disable_mocked_server" {
  type    = bool
  default = false
}

# use ssm_validation instead of validator
variable "enable_ssm_validate" {
  type    = bool
  default = false
}
