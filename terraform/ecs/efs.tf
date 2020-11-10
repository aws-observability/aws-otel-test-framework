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

# we are creating a efs system in order to mount some data/files into the ecs tasks.
# basically we mount two files into the collector task.
# 1. /etc/hosts, we will bind "mocked-server" to "127.0.0.1", because the certificate is signed by the domain name.
# 2. /etc/ssl/cert.pem, we will need this file to fake the root cert for the mocked server.

## create a efs system
locals {
  efs_name = "efs-${module.common.testing_id}"
}
resource "aws_efs_file_system" "collector_efs" {
  creation_token = module.common.testing_id

  tags = {
    Name = local.efs_name
  }
}

resource "aws_efs_mount_target" "collector_efs_mount" {
  # map to all subnets
  count = 3

  file_system_id  = aws_efs_file_system.collector_efs.id
  subnet_id       = element(tolist(module.basic_components.aoc_private_subnet_ids), count.index)
  security_groups = [module.basic_components.aoc_security_group_id]

  depends_on = [aws_efs_file_system.collector_efs]
}

## create a ec2 instance with the efs system and use userdata to put files onto it
data "aws_ami" "amazonlinux2" {
  most_recent = true

  filter {
    name   = "owner-alias"
    values = ["amazon"]
  }

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm*"]
  }

  owners = ["amazon"] # Canonical
}

data "template_file" "user_data" {
  template = file("./efs_userdata.sh.tpl")

  vars = {
    cert_content = file("../../mocked_server/certificates/ssl/certificate.crt")
    efs_id = aws_efs_file_system.collector_efs.id
  }
}

resource "aws_instance" "collector_efs_ec2" {
  ami                         = data.aws_ami.amazonlinux2.id
  instance_type               = "t2.micro"
  subnet_id                   = tolist(module.basic_components.aoc_private_subnet_ids)[0]
  vpc_security_group_ids      = [module.basic_components.aoc_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = module.common.ssh_key_name
  user_data                   = data.template_file.user_data.rendered

  volume_tags = {
    Name = local.efs_name
  }

  depends_on = [aws_efs_mount_target.collector_efs_mount]
}

output "userdata" {
  value = data.template_file.user_data.rendered
}

