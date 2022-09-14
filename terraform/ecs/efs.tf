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
  subnet_id       = element(tolist(module.basic_components.aoc_public_subnet_ids), count.index)
  security_groups = [module.basic_components.aoc_security_group_id]

  depends_on = [aws_efs_file_system.collector_efs]
}

## create a ec2 instance with the efs system and use userdata to put files onto it
data "aws_ami" "amazonlinux2" {
  most_recent = true

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-2.0.????????.?-x86_64-gp2"]
  }

  owners = ["amazon"]
}

resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "aws_ssh_key" {
  key_name   = "testing-${module.common.testing_id}"
  public_key = tls_private_key.ssh_key.public_key_openssh
  tags = {
    "ephemeral" : "true",
    "TestCase" : var.testcase,
  }
}

resource "aws_instance" "collector_efs_ec2" {
  ami                         = data.aws_ami.amazonlinux2.id
  instance_type               = "t2.micro"
  subnet_id                   = tolist(module.basic_components.aoc_public_subnet_ids)[0]
  vpc_security_group_ids      = [module.basic_components.aoc_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = aws_key_pair.aws_ssh_key.key_name

  volume_tags = {
    Name = local.efs_name
  }

  tags = {
    Name = "Integ-test-aoc"
  }

  depends_on = [aws_efs_mount_target.collector_efs_mount, aws_key_pair.aws_ssh_key]
}

resource "null_resource" "mount_efs" {
  provisioner "remote-exec" {
    inline = [
      "sudo mkdir -p /efs",
      "sudo yum install amazon-efs-utils -y",
      "sudo mount -t efs ${aws_efs_file_system.collector_efs.id}:/ /efs"
    ]

    connection {
      type        = "ssh"
      user        = "ec2-user"
      private_key = tls_private_key.ssh_key.private_key_pem
      host        = aws_instance.collector_efs_ec2.public_ip
    }
  }

  depends_on = [aws_instance.collector_efs_ec2]
}
resource "null_resource" "scp_cert" {
  provisioner "file" {
    content     = module.basic_components.mocked_server_cert_content
    destination = "/tmp/ca-bundle.crt"
    connection {
      type        = "ssh"
      user        = "ec2-user"
      private_key = tls_private_key.ssh_key.private_key_pem
      host        = aws_instance.collector_efs_ec2.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      "sudo cp /tmp/ca-bundle.crt /efs/ca-bundle.crt"
    ]
    connection {
      type        = "ssh"
      user        = "ec2-user"
      private_key = tls_private_key.ssh_key.private_key_pem
      host        = aws_instance.collector_efs_ec2.public_ip
    }
  }

  depends_on = [null_resource.mount_efs]
}

output "private_key" {
  value     = tls_private_key.ssh_key.private_key_pem
  sensitive = true
}

output "efs_ip" {
  value = aws_instance.collector_efs_ec2.public_ip
}

