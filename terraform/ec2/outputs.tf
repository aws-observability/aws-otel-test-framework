output "instance_password_data" {
  value = aws_instance.aoc.password_data
}

output "private_key" {
  value = tls_private_key.ssh_key.private_key_pem
}

output "collector_instance_public_ip" {
  value = aws_instance.aoc.public_ip
}

output "collector_instance_id" {
  value = aws_instance.aoc.id
}

output "sample_app_instance_public_ip" {
  value = aws_instance.sidecar.public_ip
}