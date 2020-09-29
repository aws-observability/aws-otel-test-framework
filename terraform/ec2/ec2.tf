provider "aws" {
  region = var.region
}

data "aws_s3_bucket_object" "ssh_private_key" {
  bucket = var.sshkey_s3_bucket
  key = var.sshkey_s3_private_key
}

resource "aws_instance" "aoc" {
  ami                         = data.aws_ami.amazon-linux2.id
  instance_type               = "t2.micro"
  security_groups             = [var.security_group_name]
  associate_public_ip_address = true
  iam_instance_profile        = var.aoc_iam_role_name
  key_name                    = var.ssh_key_name

  tags = {
    Name = "terraform-integ-test"
  }

  provisioner "file" {
    source = "../config/otconfig/default.yml"
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
      "wget https://${var.package_s3_bucket}.s3.amazonaws.com/amazon_linux/amd64/${var.agent_version}/aws-observability-collector.rpm",
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

resource "aws_instance" "emitter" {
  ami                         = data.aws_ami.suse.id
  instance_type               = "t2.micro"
  security_groups             = [var.security_group_name]
  associate_public_ip_address = true
  iam_instance_profile        = var.aoc_iam_role_name
  key_name                    = var.ssh_key_name


  tags = {
    Name = "terraform-integ-test"
  }

  provisioner "remote-exec" {
    inline = [
      "sudo systemctl start docker",
      "sudo docker run -e OTEL_RESOURCE_ATTRIBUTES=service.namespace=${var.otel_service_namespace},service.name=${var.otel_service_name} -e S3_REGION=${var.region} -e TRACE_DATA_BUCKET=${var.trace_data_bucket} -e TRACE_DATA_S3_KEY=${aws_instance.aoc.id} -e INSTANCE_ID=${aws_instance.aoc.id} -e OTEL_OTLP_ENDPOINT=${aws_instance.aoc.private_ip}:55680 -d josephwy/integ-test-emitter:multiplatform"
    ]

    connection {
      type = "ssh"
      user = "ec2-user"
      private_key = data.aws_s3_bucket_object.ssh_private_key.body
      host = aws_instance.emitter.public_ip
    }
  }

  provisioner "local-exec" {
    command = var.validator_path
    environment = {
      AGENT_VERSION = var.agent_version
      REGION = var.region
      INSTANCE_ID = aws_instance.aoc.id
      EXPECTED_METRIC = "DEFAULT_EXPECTED_METRIC"
      EXPECTED_TRACE = "DEFAULT_EXPECTED_TRACE"
      TRACE_S3_BUCKET = var.trace_data_bucket
      NAMESPACE = "${var.otel_service_namespace}/${var.otel_service_name}"
    }
  }
}

output "public_ip" {
  value = aws_instance.aoc.public_ip
}

output "emitter_public_ip" {
  value = aws_instance.emitter.public_ip
}