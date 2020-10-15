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






