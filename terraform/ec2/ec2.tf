module "common" {
  source = "../common"
}

module "basic_components" {
  source = "../basic_components"
}

## get the ssh private key
data "aws_s3_bucket_object" "ssh_private_key" {
  bucket = module.common.sshkey_s3_bucket
  key = module.common.sshkey_s3_private_key
}


## launch ec2 instance to install aoc [todo, support more amis, only amazonlinux2 is supported now]
resource "aws_instance" "aoc" {
  ami                         = data.aws_ami.selected.id
  instance_type               = "t2.micro"
  subnet_id                   = module.basic_components.aoc_public_subnet_ids[0]
  vpc_security_group_ids      = [module.common.aoc_vpc_security_group]
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
  ami                         = data.aws_ami.suse.id
  instance_type               = "t2.micro"
  subnet_id                   = module.basic_components.aoc_public_subnet_ids[0]
  vpc_security_group_ids      = [module.common.aoc_vpc_security_group]
  associate_public_ip_address = true
  iam_instance_profile        = module.common.aoc_iam_role_name
  key_name                    = module.common.ssh_key_name

  provisioner "remote-exec" {
    inline = [
      "sudo systemctl start docker",
      "sudo docker run -p 4567:4567 -e OTEL_RESOURCE_ATTRIBUTES=service.namespace=${module.common.otel_service_namespace},service.name=${module.common.otel_service_name} -e INSTANCE_ID=${module.common.testing_id} -e OTEL_EXPORTER_OTLP_ENDPOINT=${aws_instance.aoc.private_ip}:55680 -d ${module.common.aoc_emitter_image}"
    ]

    connection {
      type = "ssh"
      user = "ec2-user"
      private_key = data.aws_s3_bucket_object.ssh_private_key.body
      host = aws_instance.emitter.public_ip
    }
  }

  provisioner "local-exec" {
    command = module.common.validator_path
    working_dir = "../../"
    environment = {
      AGENT_VERSION = var.aoc_version
      REGION = var.region
      INSTANCE_ID = module.common.testing_id
      EXPECTED_METRIC = "DEFAULT_EXPECTED_METRIC"
      EXPECTED_TRACE = "DEFAULT_EXPECTED_TRACE"
      NAMESPACE = "${module.common.otel_service_namespace}/${module.common.otel_service_name}"
      DATA_EMITTER_ENDPOINT = "http://${aws_instance.emitter.public_ip}:4567/span0"
    }
  }
}

output "public_ip" {
  value = aws_instance.aoc.public_ip
}

output "emitter_public_ip" {
  value = aws_instance.emitter.public_ip
}