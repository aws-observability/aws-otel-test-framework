output "default_vpc_id" {
  value = aws_default_vpc.default_vpc.id
}

output "default_subnet_ids" {
  value = data.aws_subnet_ids.default_subnet_ids.ids
}

output "aoc_security_group_id" {
  value = data.aws_security_group.aoc_security_group.id
}

output "testing_id" {
  value = local.testing_id
}

output "aoc_emitter_image" {
  value = var.data_emitter_image
}

output "aoc_image" {
  value = "${var.aoc_image_repo}:${var.aoc_version}"
}

output "aoc_iam_role_arn" {
  value = data.aws_iam_role.aoc_iam_role.arn
}

output "validator_path" {
  value = "./gradlew :validator:run"
}

output "aoc_version" {
  value = var.aoc_version
}

output "otel_service_namespace" {
  value = "YingOtel"
}

output "otel_service_name" {
  value = "Terraform"
}
