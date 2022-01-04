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

# this module is mainly to store the general constants

# generate a testing_id whenever people want to use, for example, use it as a ecs cluster to prevent cluster name conflict
resource "random_id" "testing_id" {
  byte_length = 8
}

# Get Account ID for user to easily get arn name from some resources on AWS such as the policy, name
# Documentation: https://stackoverflow.com/a/68398082
data "aws_caller_identity" "current" {}

locals {
  aws_account_id = data.aws_caller_identity.current.account_id
}