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

  aoc_version = var.aoc_version
}

locals {
  launch_date = formatdate("YYYY-MM-DD", timestamp())
}

data "aws_ecr_repository" "sample_apps" {
  name = module.common.sample_app_ecr_repo_name
}

# launch ec2
module "ec2_setup" {
  source = "../ec2"

  ami_family            = var.ami_family
  amis                  = var.amis
  testing_ami           = var.testing_ami
  aoc_version           = var.aoc_version
  region                = var.region
  testcase              = var.testcase
  sample_app_image      = var.soaking_sample_app != "" ? "${data.aws_ecr_repository.sample_apps.repository_url}:${var.soaking_sample_app}-latest" : var.soaking_sample_app_image
  sidecar_instance_type = var.sidecar_instance_type
  skip_validation       = true

  # soaking test config
  # StatsD use its own docker compose for udp port
  soaking_compose_file = fileexists("${var.testcase}/soaking_docker_compose.tpl") ? "${var.testcase}/soaking_docker_compose.tpl" : (var.sample_app_mode == "push" ? "../templates/defaults/soaking_docker_compose.tpl" : "../templates/defaults/soaking_docker_compose_pull_mode.tpl")
  soaking_data_mode    = var.soaking_data_mode
  soaking_data_rate    = var.soaking_data_rate
  soaking_data_type    = var.soaking_data_type

  cortex_instance_endpoint = var.cortex_instance_endpoint

  # negative soaking
  mock_endpoint = var.negative_soaking ? "http://127.0.0.2" : var.mock_endpoint
  mocked_server = var.mocked_server

  # install cwagent
  install_cwagent = true

  # use our own ssh key name
  ssh_key_name          = var.ssh_key_name
  sshkey_s3_bucket      = var.sshkey_s3_bucket
  sshkey_s3_private_key = var.sshkey_s3_private_key

  # additional dimension
  commit_id        = var.commit_id
  launch_date      = local.launch_date
  negative_soaking = var.negative_soaking

  install_package_source     = var.install_package_source
  install_package_local_path = var.install_package_local_path

  soaking_metric_namespace = var.soaking_metric_namespace

  debug = var.debug

  testing_type = var.testing_type

  kafka_version = var.kafka_version

  patch = var.patch
}
