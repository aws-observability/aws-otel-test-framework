output "testing_id" {
  value = local.testing_id
}

output "aoc_emitter_image" {
  value = var.data_emitter_image
}

output "aoc_image" {
  value = "${var.aoc_image_repo}:${var.aoc_version}"
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

output "ssh_key_name" {
  value = "aoc-ssh-key-for-ec2"
}

output "sshkey_s3_bucket" {
  value = "aoc-ssh-key"
}

output "sshkey_s3_private_key" {
  value = "aoc-ssh-private"
}

output "aoc_iam_role_name" {
  value = "aoc-e2e-iam-role"
}

output "aoc_vpc_name" {
  value = "aoc-vpc"
}

output "aoc_vpc_security_group" {
  value = "aoc-vpc-security-group"
}
