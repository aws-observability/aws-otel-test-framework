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

  aoc_version = var.aoc_version
}

module "basic_components" {
  source = "../basic_components"

  region = var.region

  testcase = var.testcase

  testing_id = module.common.testing_id

  mocked_endpoint = var.mock_endpoint

  sample_app = var.sample_app
}

provider "aws" {
  region  = var.region
}

data "aws_ecr_repository" "sample_app" {
  name = module.common.sample_app_ecr_repo_name
}

# get ami object
locals {
  docker_compose_path = var.soaking_compose_file != "" ? var.soaking_compose_file : fileexists("${var.testcase}/docker_compose.tpl") ? "${var.testcase}/docker_compose.tpl" : module.common.default_docker_compose_path
  selected_ami = var.amis[var.testing_ami]
  ami_family = var.ami_family[local.selected_ami["family"]]
  ami_id = data.aws_ami.selected.id
  instance_type = local.ami_family["instance_type"]
  otconfig_destination = local.ami_family["otconfig_destination"]
  login_user = local.ami_family["login_user"]
  connection_type = local.ami_family["connection_type"]
  user_data = local.ami_family["user_data"]
  download_command = format(local.ami_family["download_command_pattern"], "https://${var.package_s3_bucket}.s3.amazonaws.com/${local.selected_ami["family"]}/${local.selected_ami["arch"]}/${var.aoc_version}/${local.ami_family["install_package"]}")

  sample_app_image = var.sample_app_image != "" ? var.sample_app_image : module.basic_components.sample_app_image
  mocked_server_image = var.mocked_server_image != "" ? var.mocked_server_image : module.basic_components.mocked_server_image

  # get ecr login domain
  ecr_login_domain = split("/", data.aws_ecr_repository.sample_app.repository_url)[0]
}

## get the ssh private key
resource "tls_private_key" "ssh_key" {
  algorithm   = "RSA"
  rsa_bits    = 4096
}

resource "aws_key_pair" "aws_ssh_key" {
  key_name = "testing-${module.common.testing_id}"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

## launch a sidecar instance to install data emitter and the mocked server
resource "aws_instance" "sidecar" {
  ami                         = data.aws_ami.suse.id
  instance_type               = "t2.micro"
  subnet_id                   = tolist(module.basic_components.aoc_public_subnet_ids)[0]
  vpc_security_group_ids      = [module.basic_components.aoc_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = aws_key_pair.aws_ssh_key.key_name

}

# launch ec2 instance to install aoc [todo, support more amis, only amazonlinux2 ubuntu, windows2019 is supported now]
resource "aws_instance" "aoc" {
  ami                         = local.ami_id
  instance_type               = local.instance_type
  subnet_id                   = tolist(module.basic_components.aoc_public_subnet_ids)[0]
  vpc_security_group_ids      = [module.basic_components.aoc_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = aws_key_pair.aws_ssh_key.key_name
  get_password_data = local.connection_type == "winrm" ? true : null
  user_data = local.user_data

}

############################################
# setup mocked server cert and host binding
############################################
data "template_file" "mocked_server_cert_for_windows" {
  template = file("../../mocked_server/certificates/ssl/certificate.crt")
}
resource "null_resource" "setup_mocked_server_cert_for_windows" {
  count = local.selected_ami["family"] == "windows" ? 1 : 0

  provisioner "file" {
    content = data.template_file.mocked_server_cert_for_windows.rendered
    destination = "C:\\ca-bundle.crt"

    connection {
      type = local.connection_type
      user = local.login_user
      password = rsadecrypt(aws_instance.aoc.password_data, tls_private_key.ssh_key.private_key_pem)
      host = aws_instance.aoc.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      "echo ${aws_instance.sidecar.private_ip} mocked-server >> C:\\Windows\\System32\\drivers\\etc\\hosts",
      "powershell \"Import-Certificate -FilePath 'C:\\ca-bundle.crt' -CertStoreLocation 'Cert:\\LocalMachine\\Root' -Verbose \""
    ]

    connection {
      type = local.connection_type
      user = local.login_user
      password = rsadecrypt(aws_instance.aoc.password_data, tls_private_key.ssh_key.private_key_pem)
      host = aws_instance.aoc.public_ip
    }
  }
}

resource "null_resource" "setup_mocked_server_cert_for_linux" {
  count = local.selected_ami["family"] != "windows" ? 1 : 0
  provisioner "file" {
    content = module.basic_components.mocked_server_cert_content
    destination = "/tmp/ca-bundle.crt"

    connection {
      type = local.connection_type
      user = local.login_user
      private_key = local.connection_type == "ssh" ? tls_private_key.ssh_key.private_key_pem : null
      password = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, tls_private_key.ssh_key.private_key_pem) : null
      host = aws_instance.aoc.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      "sudo mkdir -p /etc/pki/tls/certs",
      "sudo chmod 777 /etc/pki/tls/certs/ca-bundle.crt",
      "sudo cp /tmp/ca-bundle.crt /etc/pki/tls/certs/ca-bundle.crt",
      "sudo chmod 777 /etc/hosts",
      "sudo echo '${aws_instance.sidecar.private_ip} mocked-server' >> /etc/hosts",
    ]

    connection {
      type = local.connection_type
      user = local.login_user
      private_key = tls_private_key.ssh_key.private_key_pem
      host = aws_instance.aoc.public_ip
    }
  }
}

############################################
# Start collector
###########################################
resource "null_resource" "start_collector" {

  provisioner "file" {
    content = module.basic_components.otconfig_content
    destination = local.otconfig_destination

    connection {
      type = local.connection_type
      user = local.login_user
      private_key = local.connection_type == "ssh" ? tls_private_key.ssh_key.private_key_pem : null
      password = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, tls_private_key.ssh_key.private_key_pem) : null
      host = aws_instance.aoc.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      local.download_command,
      local.ami_family["install_command"],
      local.ami_family["start_command"],
    ]

    connection {
      type = local.connection_type
      user = local.login_user
      private_key = local.connection_type == "ssh" ? tls_private_key.ssh_key.private_key_pem : null
      password = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, tls_private_key.ssh_key.private_key_pem) : null
      host = aws_instance.aoc.public_ip
    }
  }
}

########################################
# Start Sample app and mocked server
#########################################
data "template_file" "docker_compose" {
  template = file(local.docker_compose_path)

  vars = {
    region = var.region
    sample_app_image = local.sample_app_image
    sample_app_external_port = module.common.sample_app_lb_port
    sample_app_listen_address_port = module.common.sample_app_listen_address_port
    listen_address = "${module.common.sample_app_listen_address_ip}:${module.common.sample_app_listen_address_port}"
    otel_resource_attributes = "service.namespace=${module.common.otel_service_namespace},service.name=${module.common.otel_service_name}"
    testing_id = module.common.testing_id
    grpc_endpoint = "${aws_instance.aoc.private_ip}:${module.common.grpc_port}"
    udp_endpoint = "${aws_instance.aoc.private_ip}:${module.common.udp_port}"

    mocked_server_image = local.mocked_server_image
    data_mode = var.soaking_data_mode
    rate = var.soaking_data_rate
    data_type = var.soaking_data_type
  }
}

resource "null_resource" "setup_sample_app_and_mock_server" {
  provisioner "file" {
    content = data.template_file.docker_compose.rendered
    destination = "/tmp/docker-compose.yml"
    connection {
      type = "ssh"
      user = "ec2-user"
      private_key = tls_private_key.ssh_key.private_key_pem
      host = aws_instance.sidecar.public_ip
    }
  }
  provisioner "remote-exec" {
    inline = [
      "sudo curl -L 'https://github.com/docker/compose/releases/download/1.27.4/docker-compose-Linux-x86_64' -o /usr/local/bin/docker-compose",
      "sudo chmod +x /usr/local/bin/docker-compose",
      "sudo systemctl start docker",
      "sudo docker run --rm -v ~/.aws:/root/.aws amazon/aws-cli ecr get-login-password --region ${var.region} | sudo docker login --username AWS --password-stdin ${local.ecr_login_domain}",
      "sudo /usr/local/bin/docker-compose -f /tmp/docker-compose.yml up -d"
    ]

    connection {
      type = "ssh"
      user = "ec2-user"
      private_key = tls_private_key.ssh_key.private_key_pem
      host = aws_instance.sidecar.public_ip
    }
  }
}

## install cwagent on the instance to collect metric from otel-collector
data "template_file" "cwagent_config" {
  count = var.install_cwagent ? 1 : 0
  template = file(local.ami_family["soaking_cwagent_config"])

  vars = {
    soaking_metric_namespace = var.soaking_metric_namespace
  }
}

# install cwagent
resource "null_resource" "install_cwagent" {
  count = var.install_cwagent ? 1 : 0
  # Use the depends_on meta-argument to handle hidden resource dependencies that Terraform can't automatically infer.
  # Explicitly specifying a dependency is only necessary when a resource relies on some other resource's behavior but doesn't access any of that resource's data in its arguments.
  depends_on = [null_resource.start_collector]
  // copy cwagent config to the instance
  provisioner "file" {
    content = data.template_file.cwagent_config[0].rendered
    destination = local.ami_family["soaking_cwagent_config_destination"]

    connection {
      type = local.connection_type
      user = local.login_user
      private_key = local.connection_type == "ssh" ? tls_private_key.ssh_key.private_key_pem: null
      password = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, tls_private_key.ssh_key.private_key_pem) : null
      host = aws_instance.aoc.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      local.ami_family["cwagent_download_command"],
      local.ami_family["cwagent_install_command"],
      local.ami_family["cwagent_start_command"]
    ]

    connection {
      type = local.connection_type
      user = local.login_user
      private_key = local.connection_type == "ssh" ? tls_private_key.ssh_key.private_key_pem: null
      password = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, tls_private_key.ssh_key.private_key_pem) : null
      host = aws_instance.aoc.public_ip
    }
  }
}

##########################################
# Validation
##########################################
module "validator" {
  count = !var.skip_validation ? 1 : 0
  source = "../validation"

  validation_config = var.validation_config
  region = var.region
  testing_id = module.common.testing_id
  metric_namespace = "${module.common.otel_service_namespace}/${module.common.otel_service_name}"
  sample_app_endpoint = "http://${aws_instance.sidecar.public_ip}:${module.common.sample_app_lb_port}"
  mocked_server_validating_url = "http://${aws_instance.sidecar.public_ip}/check-data"
  canary = var.canary

  aws_access_key_id = var.aws_access_key_id
  aws_secret_access_key = var.aws_secret_access_key

  depends_on = [null_resource.setup_sample_app_and_mock_server]
}

output "public_ip" {
  value = aws_instance.aoc.public_ip
}

output "docker_compose" {
  value = data.template_file.docker_compose.rendered
}
