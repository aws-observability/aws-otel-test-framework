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

output "aoc_vpc_id" {
  value = data.aws_vpc.aoc_vpc.id
}

output "aoc_private_subnet_ids" {
  value = data.aws_subnet_ids.aoc_private_subnet_ids.ids
}

output "aoc_public_subnet_ids" {
  value = data.aws_subnet_ids.aoc_public_subnet_ids.ids
}

output "aoc_security_group_id" {
  value = data.aws_security_group.aoc_security_group.id
}

output "aoc_iam_role_arn" {
  value = data.aws_iam_role.aoc_iam_role.arn
}

output "otconfig_content" {
  value = data.template_file.otconfig.rendered
}