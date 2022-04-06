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

# this module is to build images for sample apps and mocked-server.
# if we make any change in sample app or mocked-server, and want to deploy them to ec2/ecs/eks,
# we need to run this module before run the tests, the tests will take the latest tag of these images.

module "common" {
  source = "../common"
}

provider "aws" {
  region = var.region
}

data "aws_ecr_repository" "sample_app" {
  name = module.common.sample_app_ecr_repo_name
}

data "aws_ecr_repository" "mocked_server" {
  name = module.common.mocked_server_ecr_repo_name
}

locals {
  # get all the sample-apps under folder ../../sample-apps
  sample_apps_dockerfile_list = tolist(fileset("../../sample-apps", "*/Dockerfile"))

  # get all the mocked_servers under folder ../../mocked_servers
  mocked_servers_dockerfile_list = tolist(fileset("../../mocked_servers", "*/Dockerfile"))

  # get ecr login domain
  ecr_login_domain = split("/", data.aws_ecr_repository.sample_app.repository_url)[0]
}

# login ecr
resource "null_resource" "login_ecr" {
  provisioner "local-exec" {
    command = "aws ecr get-login-password --region ${var.region} | docker login --username AWS --password-stdin ${local.ecr_login_domain}"
  }
}

# build and push sample apps image
resource "null_resource" "build_and_push_sample_apps" {
  count = length(local.sample_apps_dockerfile_list)

  provisioner "local-exec" {
    command = "docker build -t ${data.aws_ecr_repository.sample_app.repository_url}:${dirname(element(local.sample_apps_dockerfile_list, count.index))}-latest ../../sample-apps/${dirname(element(local.sample_apps_dockerfile_list, count.index))}/"
  }

  provisioner "local-exec" {
    command = "docker push ${data.aws_ecr_repository.sample_app.repository_url}:${dirname(element(local.sample_apps_dockerfile_list, count.index))}-latest"
  }

  depends_on = [null_resource.login_ecr]
}

# build and push mocked server image
resource "null_resource" "build_and_push_mocked_server" {
  count = length(local.mocked_servers_dockerfile_list)

  provisioner "local-exec" {
    command = "docker build -t ${data.aws_ecr_repository.mocked_server.repository_url}:${dirname(element(local.mocked_servers_dockerfile_list, count.index))}-latest ../../mocked_servers/${dirname(element(local.mocked_servers_dockerfile_list, count.index))}/"
  }

  provisioner "local-exec" {
    command = "docker push ${data.aws_ecr_repository.mocked_server.repository_url}:${dirname(element(local.mocked_servers_dockerfile_list, count.index))}-latest"
  }

  depends_on = [null_resource.login_ecr]
}

output "sample_apps_dockerfile_list" {
  value = local.sample_apps_dockerfile_list
}

output "mocked_servers_dockerfile_list" {
  value = local.mocked_servers_dockerfile_list
}