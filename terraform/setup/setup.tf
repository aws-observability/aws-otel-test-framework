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
  key_name = var.sshkey_s3_bucket
  public_key = tls_private_key.ssh_key.public_key_openssh
}

resource "aws_s3_bucket" "ssh_key" {
  bucket = var.sshkey_s3_bucket
  acl = "private"
}

resource "aws_s3_bucket_object" "ssh_key" {
  bucket = aws_s3_bucket.ssh_key.id
  key = module.common.sshkey_s3_private_key
  content = tls_private_key.ssh_key.private_key_pem
  content_type = "text/plain"
}

## create one iam role for all the tests
resource "aws_iam_instance_profile" "aoc_test_profile" {
  name = module.common.aoc_iam_role_name
  role = aws_iam_role.aoc_role.name
}

resource "aws_iam_role" "aoc_role" {
  name = module.common.aoc_iam_role_name
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

# create vpc with nat gateway so that we can use it to launch awsvpc ecs task in both ecs and fargate
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"

  name = module.common.aoc_vpc_name
  cidr = "10.0.0.0/16"

  azs             = ["${var.region}a", "${var.region}b", "${var.region}c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  enable_vpn_gateway = true

  tags = {
    Terraform = "true"
    Environment = "dev"
  }
}

resource "aws_security_group" "aoc_sg" {
  name = module.common.aoc_vpc_security_group
  vpc_id = module.vpc.vpc_id

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

