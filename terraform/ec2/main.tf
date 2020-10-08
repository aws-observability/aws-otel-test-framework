module "common" {
  source = "../common"

  data_emitter_image = var.data_emitter_image
  aoc_version = var.aoc_version
}

module "basic_components" {
  source = "../basic_components"

  region = var.region
}

provider "aws" {
  region  = var.region
}

## get the ssh private key
data "aws_s3_bucket_object" "ssh_private_key" {
  bucket = var.sshkey_s3_bucket
  key = var.sshkey_s3_private_key
}

# generate otconfig
data "template_file" "otconfig" {
  template = file(var.otconfig_path)

  vars = {
    region = var.region
    otel_service_namespace = module.common.otel_service_namespace
    otel_service_name = module.common.otel_service_name
    testing_id = module.common.testing_id
  }
}

# launch ec2 instance to install aoc [todo, support more amis, only amazonlinux2 is supported now]
resource "aws_instance" "aoc" {
  ami                         = data.aws_ami.selected.id
  instance_type               = "t2.micro"
  subnet_id                   = tolist(module.basic_components.aoc_public_subnet_ids)[0]
  vpc_security_group_ids      = [module.basic_components.aoc_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = module.common.ssh_key_name

  provisioner "file" {
    content = data.template_file.otconfig.rendered
    destination = "/tmp/ot-default.yml"

    connection {
      type = "ssh"
      user = "ec2-user"
      private_key = data.aws_s3_bucket_object.ssh_private_key.body
      host = aws_instance.aoc.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      "wget https://${var.package_s3_bucket}.s3.amazonaws.com/amazon_linux/amd64/${var.aoc_version}/aws-observability-collector.rpm",
      "sudo rpm -Uvh aws-observability-collector.rpm",
      "sudo /opt/aws/aws-observability-collector/bin/aws-observability-collector-ctl -c /tmp/ot-default.yml -a start"
    ]

    connection {
      type = "ssh"
      user = "ec2-user"
      private_key = data.aws_s3_bucket_object.ssh_private_key.body
      host = aws_instance.aoc.public_ip
    }
  }
}


## launch a ec2 instance to install data emitter
resource "aws_instance" "emitter" {
  # don't do emitter instance if the sample app is not callable
  count = var.sample_app_callable ? 1 : 0
  ami                         = data.aws_ami.suse.id
  instance_type               = "t2.micro"
  subnet_id                   = tolist(module.basic_components.aoc_public_subnet_ids)[0]
  vpc_security_group_ids      = [module.basic_components.aoc_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = module.common.ssh_key_name
}

data "template_file" "docker_compose" {
  template = file(var.docker_compose_path)

  vars = {
    data_emitter_image = var.data_emitter_image
    sample_app_listen_address_port = module.common.sample_app_listen_address_port
    listen_address = "${module.common.sample_app_listen_address_ip}:${module.common.sample_app_listen_address_port}"
    otel_resource_attributes = "service.namespace=${module.common.otel_service_namespace},service.name=${module.common.otel_service_name}"
    testing_id = module.common.testing_id
    otel_endpoint = "${aws_instance.aoc.private_ip}:55680"
  }
}
resource "null_resource" "sample-app-validator" {
  count = var.sample_app_callable ? 1 : 0

  provisioner "file" {
    content = data.template_file.docker_compose.rendered
    destination = "/tmp/docker-compose.yml"
    connection {
      type = "ssh"
      user = "ec2-user"
      private_key = data.aws_s3_bucket_object.ssh_private_key.body
      host = aws_instance.emitter[0].public_ip
    }
  }
  provisioner "remote-exec" {
    inline = [
      "sudo curl -L 'https://github.com/docker/compose/releases/download/1.27.4/docker-compose-Linux-x86_64' -o /usr/local/bin/docker-compose",
      "sudo chmod +x /usr/local/bin/docker-compose",
      "sudo systemctl start docker",
      "sudo /usr/local/bin/docker-compose -f /tmp/docker-compose.yml up -d"
    ]

    connection {
      type = "ssh"
      user = "ec2-user"
      private_key = data.aws_s3_bucket_object.ssh_private_key.body
      host = aws_instance.emitter[0].public_ip
    }
  }

  provisioner "local-exec" {
    command = "${module.common.validator_path} --args='-c ${var.validation_config} -t ${module.common.testing_id} --region ${var.region} --metric-namespace ${module.common.otel_service_namespace}/${module.common.otel_service_name} --endpoint http://${aws_instance.emitter[0].public_ip}'"
    working_dir = "../../"
  }
}

# only run it when aoc collects metrics without any sample apps
resource "null_resource" "validator" {
  count = !var.sample_app_callable ? 1 : 0
  provisioner "local-exec" {
    command = "${module.common.validator_path} --args='-c ${var.validation_config} -t ${module.common.testing_id} --region ${var.region} --metric-namespace ${module.common.otel_service_namespace}/${module.common.otel_service_name}'"
    working_dir = "../../"
  }
}

output "public_ip" {
  value = aws_instance.aoc.public_ip
}
