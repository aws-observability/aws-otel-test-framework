# in this module, we create the necessary common resources for the integ-tests, this setup module will only need to be executed once.
# vpc, iam role, security group, the number of those resources could be limited, creating them concurrently for every pr would trigger throttling issue.

module "common" {
  source = "../common"
}

provider "aws" {
  region = var.region
}

# create ssh key pair and upload them to s3
resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits = 4096
}

resource "aws_key_pair" "generated_key" {
  key_name = var.ssh_key_name
  public_key = tls_private_key.ssh_key.public_key_openssh
}

resource "aws_s3_bucket_object" "ssh_key" {
  bucket = var.sshkey_s3_bucket
  key = var.sshkey_s3_private_key
  content = tls_private_key.ssh_key.private_key_pem
  content_type = "text/plain"
}

# create one iam role for all the tests
resource "aws_iam_instance_profile" "aoc_test_profile" {
  name = var.aoc_iam_role_name
  role = aws_iam_role.aoc_role.name
}

resource "aws_iam_role" "aoc_role" {
  name = var.aoc_iam_role_name
  path = "/"

  assume_role_policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": "sts:AssumeRole",
            "Principal": {
               "Service": "ec2.amazonaws.com"
            },
            "Effect": "Allow",
            "Sid": ""
        },
        {
          "Sid": "",
          "Effect": "Allow",
          "Principal": {
            "Service": "ecs-tasks.amazonaws.com"
          },
          "Action": "sts:AssumeRole"
        }
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "ec2-read-only-policy-attachment" {
  role = aws_iam_role.aoc_role.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}

resource "aws_security_group" "sg_22_80" {
  name   = var.security_group_name
  vpc_id = module.common.default_vpc_id

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }


  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 55680
    to_port = 55680
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 4567
    to_port = 4567
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}


# create a security group for all the tests
#resource "aws_default_vpc" "default" {
#  tags = {
#    Name = "Default VPC"
#  }
#}
#
#resource "aws_security_group" "aoc_security_group" {
#  name   = var.aoc_security_group_name
#  vpc_id = aws_default_vpc.default.id
#
#  ingress {
#    from_port   = 22
#    to_port     = 22
#    protocol    = "tcp"
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#
#  ingress {
#    from_port   = 80
#    to_port     = 80
#    protocol    = "tcp"
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#
#
#  ingress {
#    from_port   = 8080
#    to_port     = 8080
#    protocol    = "tcp"
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#
#  egress {
#    from_port   = 0
#    to_port     = 0
#    protocol    = "-1"
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#}



