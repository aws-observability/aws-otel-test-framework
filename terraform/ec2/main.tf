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

  aoc_version         = var.aoc_version
  aoc_vpc_name        = var.aoc_vpc_name
  security_group_name = var.aoc_vpc_security_group
}

module "basic_components" {
  source = "../basic_components"

  region = var.region

  aoc_vpc_name           = var.aoc_vpc_name
  aoc_vpc_security_group = var.aoc_vpc_security_group

  testcase = var.testcase

  testing_id = module.common.testing_id

  mocked_endpoint = var.mock_endpoint

  sample_app = var.sample_app

  mocked_server = var.mocked_server

  cortex_instance_endpoint = var.cortex_instance_endpoint

  sample_app_listen_address_host = aws_instance.sidecar.public_ip

  sample_app_listen_address_port = module.common.sample_app_lb_port

  debug = var.debug
}

provider "aws" {
  region = var.region
}

data "aws_caller_identity" "current" {
}

data "aws_ecr_repository" "sample_app" {
  name = module.common.sample_app_ecr_repo_name
}

# get ami object
locals {
  docker_compose_path  = var.soaking_compose_file != "" ? var.soaking_compose_file : fileexists("${var.testcase}/docker_compose.tpl") ? "${var.testcase}/docker_compose.tpl" : module.common.default_docker_compose_path
  selected_ami         = var.amis[var.testing_ami]
  ami_family           = var.ami_family[local.selected_ami["family"]]
  ami_id               = data.aws_ami.selected.id
  instance_type        = lookup(local.selected_ami, "instance_type", local.ami_family["instance_type"])
  otconfig_destination = local.ami_family["otconfig_destination"]
  login_user           = lookup(local.selected_ami, "login_user", local.ami_family["login_user"])
  connection_type      = local.ami_family["connection_type"]
  user_data            = lookup(local.selected_ami, "user_data", local.ami_family["user_data"])
  download_command     = format(local.ami_family["download_command_pattern"], "https://${var.package_s3_bucket}.s3.amazonaws.com/${local.selected_ami["os_family"]}/${local.selected_ami["arch"]}/${var.aoc_version}/${local.ami_family["install_package"]}")

  sample_app_image    = var.sample_app_image != "" ? var.sample_app_image : module.basic_components.sample_app_image
  mocked_server_image = var.mocked_server_image != "" ? var.mocked_server_image : module.basic_components.mocked_server_image

  # get ecr login domain
  ecr_login_domain = split("/", data.aws_ecr_repository.sample_app.repository_url)[0]

  # get SSM package version, latest is the default version
  ssm_package_version = var.aoc_version == "latest" ? "\"\"" : var.aoc_version
  testcase_name       = split("/", var.testcase)[2]
}

## launch a sidecar instance to install data emitter and the mocked server
resource "aws_instance" "sidecar" {
  ami                         = data.aws_ami.amazonlinux2.id
  instance_type               = var.sidecar_instance_type
  subnet_id                   = module.basic_components.random_subnet_instance_id
  vpc_security_group_ids      = [module.basic_components.aoc_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = local.ssh_key_name
  tags = {
    Name      = "Integ-test-Sample-App"
    Patch     = var.patch
    TestCase  = var.testcase
    TestID    = module.common.testing_id
    ephemeral = "true"
  }

  metadata_options {
    http_endpoint = "enabled"
    http_tokens = "required"
    # Use 2 hops because some of the test services run inside docker in the instance.
    # That counts as an extra hop to access the IMDS. The default value is 1.
    http_put_response_hop_limit = 2
  }
}

# launch ec2 instance to install aoc [todo, support more amis, only amazonlinux2 ubuntu, windows2019 is supported now]
resource "aws_instance" "aoc" {
  ami                         = local.ami_id
  instance_type               = local.instance_type
  subnet_id                   = module.basic_components.random_subnet_instance_id
  vpc_security_group_ids      = [module.basic_components.aoc_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = local.ssh_key_name
  get_password_data           = local.connection_type == "winrm" ? true : null
  user_data                   = local.user_data

  tags = {
    Name      = "Integ-test-aoc"
    Patch     = var.patch
    TestCase  = var.testcase
    TestID    = module.common.testing_id
    ephemeral = "true"
  }
  
  metadata_options {
    http_endpoint = "enabled"
    http_tokens = "required"
  }
}

resource "null_resource" "check_patch" {
  depends_on = [
    aws_instance.aoc,
  aws_instance.sidecar]
  count = var.patch ? 1 : 0

  # https://discuss.hashicorp.com/t/how-to-rewrite-null-resource-with-local-exec-provisioner-when-destroy-to-prepare-for-deprecation-after-0-12-8/4580/2
  triggers = {
    sidecar_id = aws_instance.sidecar.id
    aoc_id     = aws_instance.aoc.id
    aotutil    = var.aotutil
  }

  provisioner "local-exec" {
    command = <<-EOT
     "${self.triggers.aotutil}" ssm wait-patch "${self.triggers.sidecar_id}" --ignore-error
     "${self.triggers.aotutil}" ssm wait-patch "${self.triggers.aoc_id}" --ignore-error
    EOT
  }
}

############################################
# setup mocked server cert and host binding
############################################
data "template_file" "mocked_server_cert_for_windows" {
  template = file("../../mocked_servers/https/certificates/ssl/certificate.crt")
}
resource "null_resource" "setup_mocked_server_cert_for_windows" {
  depends_on = [null_resource.check_patch]
  count      = local.selected_ami["family"] == "windows" ? 1 : 0

  provisioner "file" {
    content     = data.template_file.mocked_server_cert_for_windows.rendered
    destination = "C:\\ca-bundle.crt"

    connection {
      type     = local.connection_type
      user     = local.login_user
      password = rsadecrypt(aws_instance.aoc.password_data, local.private_key_content)
      host     = aws_instance.aoc.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      "echo ${aws_instance.sidecar.private_ip} mocked-server >> C:\\Windows\\System32\\drivers\\etc\\hosts",
      "powershell \"Import-Certificate -FilePath 'C:\\ca-bundle.crt' -CertStoreLocation 'Cert:\\LocalMachine\\Root' -Verbose \""
    ]

    connection {
      type     = local.connection_type
      user     = local.login_user
      password = rsadecrypt(aws_instance.aoc.password_data, local.private_key_content)
      host     = aws_instance.aoc.public_ip
    }
  }
}

resource "null_resource" "setup_mocked_server_cert_for_linux" {
  depends_on = [null_resource.check_patch]
  count      = local.selected_ami["family"] != "windows" ? 1 : 0
  provisioner "file" {
    content     = module.basic_components.mocked_server_cert_content
    destination = "/tmp/ca-bundle.crt"

    connection {
      type        = local.connection_type
      user        = local.login_user
      private_key = local.connection_type == "ssh" ? local.private_key_content : null
      password    = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, local.private_key_content) : null
      host        = aws_instance.aoc.public_ip
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
      type        = local.connection_type
      user        = local.login_user
      private_key = local.private_key_content
      host        = aws_instance.aoc.public_ip
    }
  }
}


############################################
# Download and Start collector
############################################
resource "null_resource" "download_collector_from_local" {
  depends_on = [null_resource.check_patch]
  count      = var.install_package_source == "local" ? 1 : 0
  provisioner "file" {
    source      = var.install_package_local_path
    destination = local.ami_family["install_package"]

    connection {
      type        = local.connection_type
      user        = local.login_user
      private_key = local.connection_type == "ssh" ? local.private_key_content : null
      password    = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, local.private_key_content) : null
      host        = aws_instance.aoc.public_ip
    }
  }
}

resource "null_resource" "download_collector_from_s3" {
  depends_on = [null_resource.check_patch]
  count      = var.install_package_source == "s3" ? 1 : 0

  provisioner "remote-exec" {
    inline = [
      local.download_command
    ]

    connection {
      type        = local.connection_type
      user        = local.login_user
      private_key = local.connection_type == "ssh" ? local.private_key_content : null
      password    = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, local.private_key_content) : null
      host        = aws_instance.aoc.public_ip
    }
  }
}

resource "null_resource" "start_collector" {
  count = var.install_package_source == "ssm" ? 0 : 1
  # either getting the install package from s3 or from local
  depends_on = [null_resource.download_collector_from_local, null_resource.download_collector_from_s3]
  provisioner "file" {
    content     = module.basic_components.otconfig_content
    destination = local.otconfig_destination

    connection {
      type        = local.connection_type
      user        = local.login_user
      private_key = local.connection_type == "ssh" ? local.private_key_content : null
      password    = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, local.private_key_content) : null
      host        = aws_instance.aoc.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      local.ami_family["wait_cloud_init"],
      local.ami_family["install_command"],
      local.ami_family["start_command"],
    ]

    connection {
      type        = local.connection_type
      user        = local.login_user
      private_key = local.connection_type == "ssh" ? local.private_key_content : null
      password    = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, local.private_key_content) : null
      host        = aws_instance.aoc.public_ip
    }
  }
}

resource "aws_ssm_parameter" "setup_aoc_config" {
  count = var.install_package_source == "ssm" ? 1 : 0
  name  = format("aoc-config-%s", module.common.testing_id)
  type  = "String"
  value = var.ssm_config
}

resource "null_resource" "install_collector_from_ssm" {
  depends_on = [null_resource.check_patch, aws_ssm_parameter.setup_aoc_config]
  count      = var.install_package_source == "ssm" ? 1 : 0

  provisioner "remote-exec" {
    inline = [
      local.ami_family["wait_cloud_init"],
    ]

    connection {
      type        = local.connection_type
      user        = local.login_user
      private_key = local.connection_type == "ssh" ? local.private_key_content : null
      password    = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, local.private_key_content) : null
      host        = aws_instance.aoc.public_ip
    }
  }

  provisioner "local-exec" {
    command = <<-EOT
        bash ../templates/local/ssm-install-aoc.sh ${aws_instance.aoc.id} ${var.ssm_package_name} ${local.ssm_package_version} ${aws_ssm_parameter.setup_aoc_config[0].name}
    EOT
  }
}

########################################
# Start Sample app and mocked server
#########################################
data "template_file" "docker_compose" {
  template = file(local.docker_compose_path)

  vars = {
    region                         = var.region
    sample_app_image               = local.sample_app_image
    sample_app_external_port       = module.common.sample_app_lb_port
    sample_app_listen_address_port = module.common.sample_app_listen_address_port
    listen_address                 = "${module.common.sample_app_listen_address_ip}:${module.common.sample_app_listen_address_port}"
    otel_resource_attributes       = "service.namespace=${module.common.otel_service_namespace},service.name=${module.common.otel_service_name}"
    testing_id                     = module.common.testing_id
    grpc_endpoint                  = "${aws_instance.aoc.private_ip}:${module.common.grpc_port}"
    udp_endpoint                   = "${aws_instance.aoc.private_ip}:${module.common.udp_port}"
    http_endpoint                  = "${aws_instance.aoc.private_ip}:${module.common.http_port}"

    mocked_server_image = local.mocked_server_image
    data_mode           = var.soaking_data_mode
    rate                = var.soaking_data_rate
    data_type           = var.soaking_data_type
  }
}

resource "null_resource" "setup_sample_app_and_mock_server" {
  count      = var.disable_mocked_server ? 0 : 1
  depends_on = [null_resource.check_patch]
  provisioner "file" {
    content     = data.template_file.docker_compose.rendered
    destination = "/tmp/docker-compose.yml"
    connection {
      type        = "ssh"
      user        = "ec2-user"
      private_key = local.private_key_content
      host        = aws_instance.sidecar.public_ip
    }
  }
  provisioner "remote-exec" {
    inline = [
      "sudo yum update -y",
      "sudo amazon-linux-extras install docker -y",
      "sudo service docker start",
      "sudo usermod -a -G docker ec2-user",
      "sudo curl -L 'https://github.com/docker/compose/releases/download/1.27.4/docker-compose-Linux-x86_64' -o /usr/local/bin/docker-compose",
      "sudo chmod +x /usr/local/bin/docker-compose",
      "sudo `aws ecr get-login --no-include-email --region ${var.region}`",
      "sleep 30", // sleep 30s to wait until dockerd is totally set up
      "sudo /usr/local/bin/docker-compose -f /tmp/docker-compose.yml up -d"
    ]

    connection {
      type        = "ssh"
      user        = "ec2-user"
      private_key = local.private_key_content
      host        = aws_instance.sidecar.public_ip
    }
  }
}

## install cwagent on the instance to collect metric from otel-collector
data "template_file" "cwagent_config" {
  count    = var.install_cwagent ? 1 : 0
  template = file(local.ami_family["soaking_cwagent_config"])

  vars = {
    soaking_metric_namespace = var.soaking_metric_namespace
    testcase                 = split("/", var.testcase)[2]
    commit_id                = var.commit_id
    launch_date              = var.launch_date
    negative_soaking         = var.negative_soaking
    data_rate                = "${var.soaking_data_mode}-${var.soaking_data_rate}"
    instance_type            = aws_instance.aoc.instance_type
    testing_ami              = var.testing_ami
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
    content     = data.template_file.cwagent_config[0].rendered
    destination = local.ami_family["soaking_cwagent_config_destination"]

    connection {
      type        = local.connection_type
      user        = local.login_user
      private_key = local.connection_type == "ssh" ? local.private_key_content : null
      password    = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, local.private_key_content) : null
      host        = aws_instance.aoc.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      local.ami_family["cwagent_download_command"],
      local.ami_family["cwagent_install_command"],
      local.ami_family["cwagent_start_command"]
    ]

    connection {
      type        = local.connection_type
      user        = local.login_user
      private_key = local.connection_type == "ssh" ? local.private_key_content : null
      password    = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, local.private_key_content) : null
      host        = aws_instance.aoc.public_ip
    }
  }
}

##########################################
# Validation
##########################################
module "validator" {
  count  = !var.skip_validation && !var.enable_ssm_validate ? 1 : 0
  source = "../validation"

  validation_config            = var.validation_config
  region                       = var.region
  testing_id                   = module.common.testing_id
  metric_namespace             = "${module.common.otel_service_namespace}/${module.common.otel_service_name}"
  sample_app_endpoint          = "http://${aws_instance.sidecar.public_ip}:${module.common.sample_app_lb_port}"
  mocked_server_validating_url = "http://${aws_instance.sidecar.public_ip}/check-data"
  canary                       = var.canary
  testcase                     = split("/", var.testcase)[2]
  cortex_instance_endpoint     = var.cortex_instance_endpoint

  account_id        = data.aws_caller_identity.current.account_id
  availability_zone = aws_instance.aoc.availability_zone

  ec2_context_json = jsonencode({
    hostId : aws_instance.aoc.id
    ami : aws_instance.aoc.ami
    name : aws_instance.aoc.private_dns
    instanceType : aws_instance.aoc.instance_type
  })

  depends_on = [null_resource.setup_sample_app_and_mock_server, null_resource.start_collector]
}

resource "null_resource" "ssm_validation" {
  depends_on = [null_resource.install_collector_from_ssm]
  count      = !var.skip_validation && var.enable_ssm_validate ? 1 : 0

  provisioner "remote-exec" {
    inline = [
      local.ami_family["status_command"],
      local.ami_family["ssm_validate"],
    ]

    connection {
      type        = local.connection_type
      user        = local.login_user
      private_key = local.connection_type == "ssh" ? local.private_key_content : null
      password    = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, local.private_key_content) : null
      host        = aws_instance.aoc.public_ip
    }
  }
}

resource "null_resource" "ssm_canary_metrics" {
  depends_on = [null_resource.ssm_validation]
  count      = !var.skip_validation && var.enable_ssm_validate && var.canary ? 1 : 0

  provisioner "local-exec" {
    command = <<-EOT
        aws cloudwatch put-metric-data --metric-name Success --dimensions testcase=${local.testcase_name} --namespace "Otel/Canary" --value 1.0
    EOT
  }
}

output "public_ip" {
  value = aws_instance.aoc.public_ip
}

output "docker_compose" {
  value = data.template_file.docker_compose.rendered
}
