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

output "testing_id" {
  value = random_id.testing_id.hex
}

output "aoc_image" {
  value = "${var.aoc_image_repo}:${var.aoc_version}"
}

output "validator_path" {
  value = "./gradlew :validator:run"
}

output "otel_service_namespace" {
  value = "aws-otel"
}

output "otel_service_name" {
  value = "aws-otel-integ-test"
}

output "aoc_iam_role_name" {
  value = "aoc-e2e-iam-role"
}

output "aoc_vpc_name" {
  value = "aoc-vpc"
}

output "aoc_vpc_security_group" {
  value = "aoc-vpc-security-group"
}

output "sample_app_container_name" {
  value = "aoc-emitter"
}

output "sample_app_listen_address_ip" {
  value = "0.0.0.0"
}

output "sample_app_listen_address_port" {
  value = "4567"
}

output "sample_app_lb_port" {
  value = "8080"
}

output "default_otconfig_path" {
  value = "../templates/defaults/otconfig.tpl"
}

output "default_eks_pod_config_path" {
  value = "../templates/defaults/eks_pod_config.tpl"
}

output "default_docker_compose_path" {
  value = "../templates/defaults/docker_compose.tpl"
}

## mocked server related
output "default_mocked_server_ecs_taskdef_path" {
  value = "../templates/defaults/mocked_server_ecs_taskdef.tpl"
}

output "mocked_server_http_port" {
  value = "8080"
}

output "mocked_server_lb_port" {
  value = "80"
}

output "grpc_port" {
  value = "4317"
}

output "udp_port" {
  value = "55690"
}

output "http_port" {
  value = "9411"
}

output "sample_app_ecr_repo_name" {
  value = "otel-test/sample-apps"
}

output "mocked_server_ecr_repo_name" {
  value = "otel-test/mocked-server"
}
