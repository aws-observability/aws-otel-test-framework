module "common" {
  source = "../common"
}

data "aws_security_group" "aoc_security_group" {
  name = module.common.aoc_vpc_security_group
}

data "aws_iam_role" "aoc_iam_role" {
  name = module.common.aoc_iam_role_name
}

data "aws_vpc" "aoc_vpc" {
  filter {
    name = "tag:Name"
    values = [module.common.aoc_vpc_name]
  }
}

# return private subnets
data "aws_subnet_ids" "aoc_private_subnet_ids" {
  vpc_id = data.aws_vpc.aoc_vpc.id
  filter {
    name   = "tag:Name"
    values = [
      "${module.common.aoc_vpc_name}-private-${var.region}a",
      "${module.common.aoc_vpc_name}-private-${var.region}b",
      "${module.common.aoc_vpc_name}-private-${var.region}c",
    ]
  }
}

# return public subnets
data "aws_subnet_ids" "aoc_public_subnet_ids" {
  vpc_id = data.aws_vpc.aoc_vpc.id
  filter {
    name   = "tag:Name"
    values = [
      "${module.common.aoc_vpc_name}-public-${var.region}a",
      "${module.common.aoc_vpc_name}-public-${var.region}b",
      "${module.common.aoc_vpc_name}-public-${var.region}c",
    ]
  }
}






