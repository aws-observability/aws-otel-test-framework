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

# data points emitted per second
variable "data_rate" {
  default = 5000
}

variable "testing_ami" {
  default = "soaking_linux"
}

variable "performance_metric_namespace" {
  default = "AWSOtelCollector/PerfTest"
}

# options: s3, local
variable "install_package_source" {
  default = "s3" # which means we download rpm/dev/msi from s3, the links are defined in the ami map.
}

# use this parameter when install_package_source is local
variable "install_package_local_path" {
  default = "../../../aws-otel-collector/build/packages/linux/amd64/aws-otel-collector.rpm"
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

# Duration to run performance test and collect metrics (in minutes)
variable "collection_period" {
  default = 10
}

variable "commit_id" {
  default = "dummy_commit"
}

variable "soaking_sample_app" {
  default = ""
}

variable "sidecar_instance_type" {
  default = "m5.2xlarge"
}

variable "kafka_version" {
  default = ""
}

variable "mock_endpoint" {
  default = "mocked-server/put-data"
}
