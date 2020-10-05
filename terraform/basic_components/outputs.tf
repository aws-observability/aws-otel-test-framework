output "aoc_vpc_id" {
  value = data.aws_vpc.aoc_vpc.id
}

output "aoc_private_subnet_ids" {
  value = data.aws_subnet_ids.aoc_private_subnet_ids.ids
}

output "aoc_public_subnet_ids" {
  value = data.aws_subnet_ids.aoc_public_subnet_ids.ids
}

output "aoc_security_group_id" {
  value = data.aws_security_group.aoc_security_group.id
}

output "aoc_iam_role_arn" {
  value = data.aws_iam_role.aoc_iam_role.arn
}