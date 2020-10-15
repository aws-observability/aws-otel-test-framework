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

variable "eks_cluster_name" {
  default = "aoc-test-eks-ec2"
}

variable "otconfig_path" {
  default = "../template/otconfig/default_otconfig.tpl"
}

variable "region" {
  default = "us-west-2"
}

variable "data_emitter_image" {
  default = "josephwy/integ-test-emitter:alpine"
}

variable "aoc_image_repo" {
  default = "611364707713.dkr.ecr.us-west-2.amazonaws.com/aws/aws-otel-collector"
}

variable "aoc_version" {
  default = "v0.1.11"
}

variable "validation_config" {
  default = "default-validation.yml"
}

variable "eks_pod_config_path" {
  default = "../template/eks-pod-config/default-eks-config.tpl"
}

# set this option to false will disable validator to call the sample app
# in some cases, it's needed, for example, ecsmetric receiver collect metric automatically even without data emitter
variable "sample_app_callable" {
  default = true
}
