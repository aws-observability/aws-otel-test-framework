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

locals {
  launch_date = formatdate("YYYY-MM-DD", timestamp())
}

# launch ec2
module "ec2_setup" {
  source = "../../ec2"

  ami_family = var.ami_family
  amis = var.amis
  testing_ami = var.testing_ami
  aoc_version = var.aoc_version
  region = var.region
  testcase = var.testcase
  sample_app_image = var.soaking_data_emitter_image
  skip_validation = true

  # soaking test config
  soaking_compose_file = "../templates/defaults/soaking_docker_compose.tpl"
  soaking_data_mode = var.soaking_data_mode
  soaking_data_rate = var.soaking_data_rate
  soaking_data_type = var.soaking_data_type

  # negative soaking
  mock_endpoint = var.negative_soaking ? "http://127.0.0.2" : "mocked-server/put-data"

  # install cwagent
  install_cwagent = true

  # use our own ssh key name
  ssh_key_name = var.ssh_key_name
  sshkey_s3_bucket = "aoc-ssh-key"
  sshkey_s3_private_key = "aoc-ssh-key-2020-07-22.pem"

  # additional dimension
  commit_id = var.commit_id
  launch_date = local.launch_date
  negative_soaking = var.negative_soaking
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
