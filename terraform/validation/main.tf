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

## any one who refer this module will trigger the validator run

locals {
  docker_compose_path = "validator_docker_compose.yml"

}

## render docker compose file
data "template_file" "docker_compose" {
  template = file("../templates/defaults/validator_docker_compose.tpl")

  vars = {
    validation_config            = var.validation_config
    testing_id                   = var.testing_id
    account_id                   = var.account_id
    region                       = var.region
    availability_zone            = var.availability_zone
    sample_app_endpoint          = var.sample_app_endpoint
    mocked_server_validating_url = var.mocked_server_validating_url
    metric_namespace             = var.metric_namespace
    canary                       = var.canary
    testcase                     = var.testcase

    # Escaping (") in the JSON when using as a terraform string variable.
    # For more information: https://www.terraform.io/docs/configuration-0-11/interpolation.html#built-in-functions
    ec2_context_json        = replace(var.ec2_context_json, "\"", "\\\"")
    ecs_context_json        = replace(var.ecs_context_json, "\"", "\\\"")
    cloudwatch_context_json = replace(var.cloudwatch_context_json, "\"", "\\\"")

    # alarm related
    cpu_alarm              = var.cpu_alarm
    mem_alarm              = var.mem_alarm
    incoming_packets_alarm = var.incoming_packets_alarm

    cortex_instance_endpoint = var.cortex_instance_endpoint
    rollup                   = var.rollup
  }

}

resource "local_file" "docker_compose_file" {
  content = data.template_file.docker_compose.rendered

  filename = local.docker_compose_path

  depends_on = [data.template_file.docker_compose]
}

resource "null_resource" "validator" {
  provisioner "local-exec" {
    command = <<-EOT
      docker-compose -f ${local.docker_compose_path} down
      docker-compose -f ${local.docker_compose_path} build
      docker-compose -f ${local.docker_compose_path} up --abort-on-container-exit
    EOT
  }

  depends_on = [local_file.docker_compose_file]
}
