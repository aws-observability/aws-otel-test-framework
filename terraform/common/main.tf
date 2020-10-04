resource "aws_default_vpc" "default_vpc" {
  tags = {
    Name = "Default VPC"
  }
}

data "aws_subnet_ids" "default_subnet_ids" {
  vpc_id = aws_default_vpc.default_vpc.id
}

data "aws_security_group" "aoc_security_group" {
  name = var.security_group_name
}

data "aws_iam_role" "aoc_iam_role" {
  name = var.aoc_iam_role
}

locals {
  # generate a testing_id whenever people want to use, for example, use it as a ecs cluster to prevent cluster name conflict
  testing_id = formatdate("YYYYMMDDhhmmss", timestamp())
}
