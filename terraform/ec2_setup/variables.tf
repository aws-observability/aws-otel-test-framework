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

variable "testing_ami" {
}

variable "sidecar_instance_type" {
  default = "t3.medium"
}

variable "soaking_sample_app_image" {
  default = "public.ecr.aws/aws-otel-test/aws-otel-load-generator:v0.11.0"
}

variable "soaking_sample_app" {
  default = ""
}

variable "soaking_compose_file" {
  default = "../templates/defaults/soaking_docker_compose.tpl"
}

# data points emitted per second
variable "soaking_data_rate" {
  default = 1000
}

variable "negative_soaking" {
  default = false
}

# if ssh_key_name is empty, we create private key every time we create instance.
# if not, we pull the private key from s3.
variable "ssh_key_name" {
}

variable "sshkey_s3_bucket" {
}

variable "sshkey_s3_private_key" {
}

# this commit id will be used as a dimension so that we can track metrics
variable "commit_id" {
  default = "dummy_commit"
}

# options: s3, local
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

variable "patch" {
  default = true
}
