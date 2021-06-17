locals {
  extra_app_image_repo = var.ecs_extra_apps_image_repo != "" ? var.ecs_extra_apps_image_repo : module.basic_components.sample_app_image_repo
}

data "template_file" "extra_apps_defs" {
  for_each = var.ecs_extra_apps
  template = file("${var.testcase}/${each.value.definition}")
  vars = {
    region     = var.region
    image_repo = local.extra_app_image_repo
  }
}

resource "aws_ecs_task_definition" "extra_apps" {
  for_each              = var.ecs_extra_apps
  family                = "taskdef-${module.common.testing_id}-${each.value.service_name}"
  container_definitions = data.template_file.extra_apps_defs[each.key].rendered
  network_mode          = each.value.network_mode
  cpu                   = each.value.cpu
  memory                = each.value.memory
  task_role_arn         = module.basic_components.aoc_iam_role_arn
  execution_role_arn    = module.basic_components.aoc_iam_role_arn
}

resource "aws_ecs_service" "extra_apps" {
  for_each         = var.ecs_extra_apps
  name             = "aocservice-${module.common.testing_id}-${each.value.service_name}"
  cluster          = module.ecs_cluster.cluster_id
  task_definition  = "${aws_ecs_task_definition.extra_apps[each.key].family}:1"
  desired_count    = each.value.replicas
  launch_type      = each.value.launch_type
  platform_version = each.value.launch_type == "FARGATE" ? "1.4.0" : null

  // NOTE: network configuration is only allowed for awsvpc
  // a hack for optional block https://github.com/hashicorp/terraform/issues/19898
  dynamic "network_configuration" {
    for_each = each.value.network_mode == "awsvpc" ? list(each.value.network_mode) : []
    content {
      subnets = module.basic_components.aoc_private_subnet_ids
      security_groups = [
      module.basic_components.aoc_security_group_id]
    }
  }
}

output "extra_apps_defs_rendered" {
  value = {
    for k, v in data.template_file.extra_apps_defs : k => v.rendered
  }
}