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

output "ami_family" {
  value = var.ami_family[var.amis[var.testing_ami]["family"]]
}

output "collector_instance_public_ip" {
  value = module.ec2_setup.collector_instance_public_ip
}

output "collector_instance_id" {
  value = module.ec2_setup.collector_instance_id
}

output "sample_app_instance_public_ip" {
  value = module.ec2_setup.sample_app_instance_public_ip
}

output "testing_id" {
  value = module.ec2_setup.testing_id
}

output "negative_soaking" {
  value = var.negative_soaking
}

output "commit_id" {
  value = var.commit_id
}

output "launch_date" {
  value = local.launch_date
}
