output "collector_instance_public_ip" {
  value = aws_instance.aoc.public_ip
}

output "collector_instance_type" {
  value = aws_instance.aoc.instance_type
}

output "collector_instance_id" {
  value = aws_instance.aoc.id
}

output "sample_app_instance_public_ip" {
  value = aws_instance.sidecar.public_ip
}

output "sample_app_instance_id" {
  value = aws_instance.sidecar.id
}

output "testing_id" {
  value = module.common.testing_id
}


