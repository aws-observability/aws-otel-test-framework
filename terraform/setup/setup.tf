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

# in this module, we create the necessary common resources for the integ-tests, this setup module will only need to be executed once.
# vpc, iam role, security group, the number of those resources could be limited, creating them concurrently for every pr would trigger throttling issue.

module "common" {
  source = "../common"
}
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.8.0"
    }
  }
}

provider "aws" {
  region = var.region
}

# create ssh key pair and upload them to s3
resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

data "aws_caller_identity" "current" {
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
  role       = aws_iam_role.aoc_role.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}

# create vpc with nat gateway so that we can use it to launch awsvpc ecs task in both ecs and fargate
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"

  name = module.common.aoc_vpc_name
  cidr = "10.0.0.0/16"

  azs             = ["${var.region}a", "${var.region}b", "${var.region}c"]
  private_subnets = ["10.0.0.0/19", "10.0.32.0/19", "10.0.64.0/19"]
  public_subnets  = ["10.0.128.0/19", "10.0.160.0/19", "10.0.192.0/19"]

  enable_nat_gateway = true
  enable_vpn_gateway = true

  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Terraform   = "true"
    Environment = "dev"
  }
}

resource "aws_security_group" "aoc_sg" {
  name   = module.common.aoc_vpc_security_group
  vpc_id = module.vpc.vpc_id

  # Allow all TCP ingress within the VPC so prometheus scrape can work with private IP.
  # https://stackoverflow.com/questions/49995417/self-reference-not-allowed-in-security-group-definition
  ingress {
    from_port = 0
    to_port   = 65535
    protocol  = "tcp"
    self      = true
  }

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
    from_port   = 443
    to_port     = 443
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
    from_port   = 55671
    to_port     = 55671
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 55671
    to_port     = 55671
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 4317
    to_port     = 4317
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 55690
    to_port     = 55690
    protocol    = "udp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 4567
    to_port     = 4567
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 5985
    to_port     = 5985
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 3389
    to_port     = 3389
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # efs
  ingress {
    from_port   = 2049
    to_port     = 2049
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # zipkin/jaeger
  ingress {
    from_port   = 9411
    to_port     = 9411
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

}

resource "aws_ecr_repository" "sample_app_ecr_repo" {
  name = module.common.sample_app_ecr_repo_name
}

resource "aws_ecr_repository" "mocked_server_ecr_repo" {
  name = module.common.mocked_server_ecr_repo_name
}

#Create S3 bucket to record terraform state in order for us to easier destroy or easier in managing what resources have been created during
#the integration test
#Document: https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket
resource "aws_s3_bucket" "terrafrom-state" {
  bucket = "${module.common.terraform_state_s3_bucket_name}${var.bucketUUID}"
}

resource "aws_s3_bucket_acl" "acl-terrafrom-state" {
  bucket = "${module.common.terraform_state_s3_bucket_name}${var.bucketUUID}"
  acl    = "private"
}

resource "aws_s3_bucket_versioning" "versioning-terrafrom-state" {
  bucket = "${module.common.terraform_state_s3_bucket_name}${var.bucketUUID}"
  versioning_configuration {
    status = "Enabled"
  }
}


resource "aws_prometheus_workspace" "amp_testing_framework" {
  alias = module.common.amp_testing_framework

}

