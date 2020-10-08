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
    source = var.otconfig_path
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
  # don't do emitter instance if there's no sample app image
  count = var.data_emitter_image != "" ? 1 : 0
  ami                         = data.aws_ami.suse.id
  instance_type               = "t2.micro"
  subnet_id                   = tolist(module.basic_components.aoc_public_subnet_ids)[0]
  vpc_security_group_ids      = [module.basic_components.aoc_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = module.common.ssh_key_name

  provisioner "remote-exec" {
    inline = [
      "sudo systemctl start docker",
      "sudo docker run -p 80:${module.common.sample_app_listen_address_port} -e S3_REGION=${var.region} -e LISTEN_ADDRESS='${module.common.sample_app_listen_address_ip}:${module.common.sample_app_listen_address_port}' -e OTEL_RESOURCE_ATTRIBUTES=service.namespace=${module.common.otel_service_namespace},service.name=${module.common.otel_service_name} -e INSTANCE_ID=${module.common.testing_id} -e OTEL_EXPORTER_OTLP_ENDPOINT=${aws_instance.aoc.private_ip}:55680 -d ${module.common.aoc_emitter_image} ${var.data_emitter_image_command}"
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
  count = var.data_emitter_image == "" ? 1 : 0
  provisioner "local-exec" {
    command = "${module.common.validator_path} --args='-c ${var.validation_config} -t ${module.common.testing_id} --region ${var.region} --metric-namespace ${module.common.otel_service_namespace}/${module.common.otel_service_name}'"
    working_dir = "../../"
  }
}

output "public_ip" {
  value = aws_instance.aoc.public_ip
}
