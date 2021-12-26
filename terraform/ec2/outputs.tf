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

output "collector_instance_public_ip" {
  value = aws_instance.aoc.public_ip
}

output "collector_instance_type" {
  value = aws_instance.aoc.instance_type
}

output "collector_instance_id" {
  value = aws_instance.aoc.id
}

output "sample_app_instance_public_ip" {
  value = aws_instance.sidecar.public_ip
}

output "sample_app_instance_id" {
  value = aws_instance.sidecar.id
}

output "testing_id" {
  value = module.common.testing_id
}

output "otconfig_content" {
  value = module.basic_components.otconfig_content
}
