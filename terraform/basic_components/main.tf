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
}

data "aws_security_group" "aoc_security_group" {
  name = module.common.aoc_vpc_security_group
}

data "aws_iam_role" "aoc_iam_role" {
  name = module.common.aoc_iam_role_name
}

data "aws_vpc" "aoc_vpc" {
  filter {
    name = "tag:Name"
    values = [module.common.aoc_vpc_name]
  }
}

# return private subnets
data "aws_subnet_ids" "aoc_private_subnet_ids" {
  vpc_id = data.aws_vpc.aoc_vpc.id
  filter {
    name   = "tag:Name"
    values = [
      "${module.common.aoc_vpc_name}-private-${var.region}a",
      "${module.common.aoc_vpc_name}-private-${var.region}b",
      "${module.common.aoc_vpc_name}-private-${var.region}c",
    ]
  }
}

# return public subnets
data "aws_subnet_ids" "aoc_public_subnet_ids" {
  vpc_id = data.aws_vpc.aoc_vpc.id
  filter {
    name   = "tag:Name"
    values = [
      "${module.common.aoc_vpc_name}-public-${var.region}a",
      "${module.common.aoc_vpc_name}-public-${var.region}b",
      "${module.common.aoc_vpc_name}-public-${var.region}c",
    ]
  }
}

locals {
  otconfig_path = fileexists("${var.testcase}/otconfig.tpl") ? "${var.testcase}/otconfig.tpl" : module.common.default_otconfig_path
}

# generate otconfig
data "template_file" "otconfig" {
  template = file(local.otconfig_path)

  vars = {
    region = var.region
    otel_service_namespace = module.common.otel_service_namespace
    otel_service_name = module.common.otel_service_name
    testing_id = var.testing_id
    grpc_port = module.common.grpc_port
    udp_port = module.common.udp_port
    cortex_instance_endpoint = module.common.cortex_instance_endpoint
    sample_app_listen_address_ip = module.common.sample_app_listen_address_ip
    sample_app_listen_address_port = module.common.sample_app_listen_address_port
    sample_app_lb_port = module.common.sample_app_lb_port

    mock_endpoint = var.mocked_endpoint
  }
}

data "template_file" "mocked_server_cert" {
  template = file("../../mocked_server/certificates/ssl/ca-bundle.crt")
}

data "aws_ecr_repository" "sample_apps" {
  name = module.common.sample_app_ecr_repo_name
}

data "aws_ecr_repository" "mocked_server" {
  name = module.common.mocked_server_ecr_repo_name
}


