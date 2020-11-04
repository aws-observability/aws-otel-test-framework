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

# this test assumes it's running on a ubuntu host
module "common" {
  source = "../common"

  data_emitter_image = var.data_emitter_image
}

# render otconfig
locals {
  otconfig_template_path = fileexists("${var.testcase}/otconfig.tpl") ? "${var.testcase}/otconfig.tpl" : module.common.default_otconfig_path
  docker_compose_template_path = "../templates/local/docker_compose.tpl"

  otconfig_file_path = "./otconfig.yml"
  docker_compose_path = "./docker_compose.yml"

  mock_endpoint = "mocked-server"
  sample_app_listen_address = "${module.common.sample_app_listen_address_ip}:${module.common.sample_app_listen_address_port}"
}

# generate otconfig
data "template_file" "otconfig" {
  template = file(local.otconfig_template_path)

  vars = {
    region = var.region
    otel_service_namespace = module.common.otel_service_namespace
    otel_service_name = module.common.otel_service_name
    testing_id = module.common.testing_id
    grpc_port = module.common.grpc_port
    udp_port = module.common.udp_port
    mock_endpoint = local.mock_endpoint
  }
}

resource "local_file" "write_otconfig_file" {
  content = data.template_file.otconfig.rendered
  filename = local.otconfig_file_path
}


# generate docker compose file
data "template_file" "docker_compose" {
  template = file(local.docker_compose_template_path)

  vars = {
    collector_repo_path = var.collector_repo_path
    otconfig_path = local.otconfig_file_path
    grpc_port = module.common.grpc_port
    udp_port = module.common.udp_port
    data_emitter_image = var.data_emitter_image
    sample_app_external_port = module.common.sample_app_listen_address_port
    sample_app_listen_address_port = module.common.sample_app_listen_address_port
    sample_app_listen_address = local.sample_app_listen_address
    otel_resource_attributes = "service.namespace=${module.common.otel_service_namespace},service.name=${module.common.otel_service_name}"
    testing_id = module.common.testing_id
    region = var.region
  }
}

resource "local_file" "write_docker_compose_file" {
  content = data.template_file.docker_compose.rendered
  filename = local.docker_compose_path
}

# launch docker compose
resource "null_resource" "run_docker_compose" {
  provisioner "local-exec" {
    command = <<-EOT
      docker-compose -f ${local.docker_compose_path} down
      docker-compose -f ${local.docker_compose_path} build
      docker-compose -f ${local.docker_compose_path} up -d
    EOT
  }
}

resource "null_resource" "validate" {
  depends_on = [null_resource.run_docker_compose]
  provisioner "local-exec" {
    command = "${module.common.validator_path} --args='-c ${var.validation_config} -t ${module.common.testing_id} --region ${var.region} --endpoint http://127.0.0.1:${module.common.sample_app_listen_address_port} --mocked-server-validating-url http://127.0.0.1/check-data'"
    working_dir = "../../"
  }
}