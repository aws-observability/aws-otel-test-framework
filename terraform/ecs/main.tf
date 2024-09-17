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

module "common" {
  source = "../common"

  aoc_image_repo = var.aoc_image_repo
  aoc_version    = var.aoc_version
}


module "aoc_msk_cluster" {
  source = "../data_aoc_msk"

  cluster_version = var.kafka_version
  testcase        = var.testcase
  dedup_topic     = "ecs"
}

module "basic_components" {
  source = "../basic_components"

  region = var.region

  testcase = var.testcase

  testing_id = module.common.testing_id

  mocked_endpoint = replace(var.mock_endpoint, "mocked-server", "localhost")

  sample_app = var.sample_app

  mocked_server = var.mocked_server

  sample_app_listen_address_host = module.common.sample_app_listen_address_ip

  sample_app_listen_address_port = module.common.sample_app_listen_address_port

  cortex_instance_endpoint = var.cortex_instance_endpoint

  extra_data = { msk = module.aoc_msk_cluster.cluster_data }

  debug = var.debug
}

module "remote_configuration" {
  count  = var.configuration_source != "env_var" ? 1 : 0
  source = "../remote_configuration"

  content    = module.basic_components.otconfig_content
  scheme     = var.configuration_source
  testing_id = module.common.testing_id
}

locals {
  ecs_taskdef_path        = fileexists("${var.testcase}/ecs_taskdef.tpl") ? "${var.testcase}/ecs_taskdef.tpl" : "../templates/${var.ecs_taskdef_directory}/ecs_taskdef.tpl"
  sample_app_image        = var.sample_app_image != "" ? var.sample_app_image : module.basic_components.sample_app_image
  mocked_server_image     = var.mocked_server_image != "" ? var.mocked_server_image : module.basic_components.mocked_server_image
  cloudwatch_context_path = fileexists("${var.testcase}/cloudwatch_context.json") ? "${var.testcase}/cloudwatch_context.json" : "../templates/${var.ecs_taskdef_directory}/cloudwatch_context.json"
  configuration_uri       = var.configuration_source != "env_var" ? module.remote_configuration[0].configuration_uri : ""
}

provider "aws" {
  region = var.region
}

data "aws_caller_identity" "current" {
}

# Builds the ecs cluster to run the tests.  Name must start with "aoc-testing" to be picked up by resource cleaner
resource "aws_ecs_cluster" "ecscluster" {
  name = "aoc-testing-${module.common.testing_id}"
  tags = {
    creation_time_rfc3339 = module.common.start_time_3339
    ephemeral             = "true"
  }
  depends_on = [
    null_resource.iam_wait
  ]
}

resource "aws_ecs_cluster_capacity_providers" "clustercapacity" {
  cluster_name = aws_ecs_cluster.ecscluster.name

  capacity_providers = [aws_ecs_capacity_provider.capacityprovider.name]

}

resource "aws_ecs_capacity_provider" "capacityprovider" {
  name = "capacityprovider-${module.common.testing_id}"

  auto_scaling_group_provider {
    auto_scaling_group_arn = aws_autoscaling_group.clusterasg.arn
  }
}

data "aws_ami" "amazon_linux_2" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-ecs-hvm-*-x86_64-ebs"]
  }
}

# The ec2 launch template used to specify the details for the ecs instances like the metdata options, ami, instance type, and much more.
resource "aws_launch_template" "launchtemp" {
  name_prefix   = "launchtemp-${module.common.testing_id}-"
  image_id      = data.aws_ami.amazon_linux_2.image_id
  instance_type = "t3.medium"
  user_data     = base64encode("#!/bin/bash\necho ECS_CLUSTER=aoc-testing-${module.common.testing_id} >> /etc/ecs/ecs.config")
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
    instance_metadata_tags      = "enabled"
  }
  network_interfaces {
    associate_public_ip_address = true
    security_groups             = [module.basic_components.aoc_security_group_id]
  }
  iam_instance_profile {
    name = aws_iam_instance_profile.cluster.name
  }
  tag_specifications {
    resource_type = "instance"

    tags = {
      Name      = "Integ-test-ecs-instance"
      Patch     = var.patch
      TestCase  = var.testcase
      TestID    = module.common.testing_id
      ephemeral = "true"
    }
  }
  tags = {
    TestCase  = var.testcase
    TestID    = module.common.testing_id
    ephemeral = "true"
  }
  depends_on = [
    null_resource.iam_wait
  ]
}

# The autoscaling group to handle the ecs instances.  The name of the autoscaling group must be unique and the "Component" tag must be set to "aoc" to be picked up by resource cleaner.
resource "aws_autoscaling_group" "clusterasg" {
  name_prefix         = "clusterasg-${module.common.testing_id}-"
  vpc_zone_identifier = module.basic_components.aoc_private_subnet_ids
  desired_capacity    = 1
  max_size            = 10
  min_size            = 1
  tags = [
    {
      "key"                 = "ephemeral"
      "value"               = "true"
      "propagate_at_launch" = false
    },
    {
      "key"                 = "Component"
      "value"               = "aoc"
      "propagate_at_launch" = false
    },
  ]
  launch_template {
    id      = aws_launch_template.launchtemp.id
    version = "$Latest"
  }
  lifecycle {
    create_before_destroy = true
  }
}

data "aws_instances" "ecs-instances" {
  instance_tags = {
    Patch = var.patch
  }

  filter {
    name   = "tag:Name"
    values = ["Integ-test-ecs-instance"]
  }

  instance_state_names = ["running"]

  depends_on = [aws_autoscaling_group.clusterasg]
}

data "aws_iam_policy_document" "ecs_agent" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cluster_instance_role" {
  assume_role_policy = data.aws_iam_policy_document.ecs_agent.json

}

resource "aws_iam_policy" "cluster_instance_policy" {
  policy = file("instance-policy.json")
}

resource "aws_iam_policy_attachment" "cluster_instance_policy_attachment" {
  name = "clusterpolicyattach"

  roles      = [aws_iam_role.cluster_instance_role.id]
  policy_arn = aws_iam_policy.cluster_instance_policy.arn
}

resource "aws_iam_instance_profile" "cluster" {
  role = aws_iam_role.cluster_instance_role.name
}

resource "null_resource" "iam_wait" {
  depends_on = [
    aws_iam_role.cluster_instance_role,
    aws_iam_policy.cluster_instance_policy,
    aws_iam_policy_attachment.cluster_instance_policy_attachment,
    aws_iam_instance_profile.cluster,
  ]

  provisioner "local-exec" {
    command = "echo iam_wait done"
  }
}

# This is a hack for known issue https://github.com/hashicorp/terraform-provider-aws/issues/4852
# We always create ECS cluster with active EC2 instances, so when destroy we need to scale down
# the asg so the cluster can be destroyed.
resource "null_resource" "scale_down_asg" {
  # https://discuss.hashicorp.com/t/how-to-rewrite-null-resource-with-local-exec-provisioner-when-destroy-to-prepare-for-deprecation-after-0-12-8/4580/2
  triggers = {
    asg_name = aws_autoscaling_group.clusterasg.name
  }

  # Only run during destroy, do nothing for apply.
  provisioner "local-exec" {
    when    = destroy
    command = <<-EOT
    aws autoscaling update-auto-scaling-group --auto-scaling-group-name "${self.triggers.asg_name}" --min-size 0 --desired-capacity 0
EOT
  }
}

resource "aws_ssm_parameter" "otconfig" {
  name  = "otconfig-${module.common.testing_id}"
  type  = "String"
  value = module.basic_components.otconfig_content
  tier  = "Advanced" // need advanced for a long list of prometheus relabel config
}

resource "null_resource" "check_patch" {
  depends_on = [
  data.aws_instances.ecs-instances, aws_instance.collector_efs_ec2]
  count = var.patch ? 1 : 0

  # https://discuss.hashicorp.com/t/how-to-rewrite-null-resource-with-local-exec-provisioner-when-destroy-to-prepare-for-deprecation-after-0-12-8/4580/2
  triggers = {
    ecs_id  = data.aws_instances.ecs-instances.ids[0]
    aoc_id  = aws_instance.collector_efs_ec2.id
    aotutil = var.aotutil
  }

  provisioner "local-exec" {
    command = <<-EOT
     "${self.triggers.aotutil}" ssm wait-patch "${self.triggers.ecs_id}" --ignore-error
     "${self.triggers.aotutil}" ssm wait-patch "${self.triggers.aoc_id}" --ignore-error
    EOT
  }
}

## create task def
data "template_file" "task_def" {
  template = file(local.ecs_taskdef_path)

  vars = {
    region                         = var.region
    aoc_image                      = module.common.aoc_image
    data_emitter_image             = local.sample_app_image
    testing_id                     = module.common.testing_id
    otel_service_namespace         = module.common.otel_service_namespace
    otel_service_name              = module.common.otel_service_name
    ssm_parameter_arn              = aws_ssm_parameter.otconfig.name
    sample_app_container_name      = module.common.sample_app_container_name
    sample_app_listen_address      = "${module.common.sample_app_listen_address_ip}:${module.common.sample_app_listen_address_port}"
    sample_app_listen_address_host = module.common.sample_app_listen_address_ip
    sample_app_listen_port         = module.common.sample_app_listen_address_port
    udp_port                       = module.common.udp_port
    grpc_port                      = module.common.grpc_port
    http_port                      = module.common.http_port
    configuration_uri              = local.configuration_uri

    mocked_server_image = local.mocked_server_image
  }
}



# debug
output "rendered" {
  value = data.template_file.task_def.rendered
}

resource "aws_ecs_task_definition" "aoc" {
  count                 = var.ecs_taskdef_network_mode == "awsvpc" ? 1 : 0
  family                = "taskdef-${module.common.testing_id}"
  container_definitions = data.template_file.task_def.rendered
  network_mode          = var.ecs_taskdef_network_mode

  requires_compatibilities = ["EC2", "FARGATE"]
  cpu                      = 256
  memory                   = 512

  # simply use one role for task role and execution role,
  # we could separate them in the future if
  # we want to limit the permissions of the roles
  task_role_arn      = module.basic_components.aoc_iam_role_arn
  execution_role_arn = module.basic_components.aoc_iam_role_arn

  # mount efs
  volume {
    name = "efs"

    efs_volume_configuration {
      file_system_id = aws_efs_file_system.collector_efs.id
      root_directory = "/"
    }
  }
  depends_on = [null_resource.mount_efs]
}

resource "aws_ecs_task_definition" "aoc_bridge" {
  count                 = var.ecs_taskdef_network_mode == "bridge" ? 1 : 0
  family                = "taskdef-${module.common.testing_id}"
  container_definitions = data.template_file.task_def.rendered
  network_mode          = var.ecs_taskdef_network_mode

  requires_compatibilities = ["EC2"]
  cpu                      = 256
  memory                   = 512

  # simply use one role for task role and execution role,
  # we could separate them in the future if
  # we want to limit the permissions of the roles
  task_role_arn      = module.basic_components.aoc_iam_role_arn
  execution_role_arn = module.basic_components.aoc_iam_role_arn

  # mount proc
  volume {
    name      = "proc"
    host_path = "/proc"
  }
  # mount dev
  volume {
    name      = "dev"
    host_path = "/dev"
  }
  # mount al1_cgroup
  volume {
    name      = "al1_cgroup"
    host_path = "/cgroup"
  }
  # mount al2_cgroup
  volume {
    name      = "al2_cgroup"
    host_path = "/sys/fs/cgroup"
  }

  # mount efs
  volume {
    name = "efs"

    efs_volume_configuration {
      file_system_id = aws_efs_file_system.collector_efs.id
      root_directory = "/"
    }
  }
  depends_on = [null_resource.mount_efs]
}

## create elb
## quota for nlb: https://docs.aws.amazon.com/elasticloadbalancing/latest/network/load-balancer-limits.html
## 50 per region, looks enough
resource "aws_lb" "aoc_lb" {
  # don't do lb if the sample app is not callable
  count = var.sample_app_callable ? 1 : 0

  # use public subnet to make the lb accessible from public internet
  subnets         = module.basic_components.aoc_public_subnet_ids
  security_groups = [module.basic_components.aoc_security_group_id]
  name            = "aoc-lb-${module.common.testing_id}"
}

resource "aws_lb_target_group" "aoc_lb_tg" {
  # don't do lb if the sample app is not callable
  count = var.sample_app_callable ? 1 : 0

  name        = "aoc-lbtg-${module.common.testing_id}"
  port        = module.common.sample_app_listen_address_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = module.basic_components.aoc_vpc_id

  health_check {
    path                = "/"
    unhealthy_threshold = 10
    healthy_threshold   = 2
    interval            = 10
    matcher             = "200,404"
  }
}

resource "aws_lb_listener" "aoc_lb_listener" {
  # don't do lb if the sample app is not callable
  count = var.sample_app_callable ? 1 : 0

  load_balancer_arn = aws_lb.aoc_lb[0].arn
  port              = module.common.sample_app_lb_port
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.aoc_lb_tg[0].arn
  }
}

## deploy
resource "aws_ecs_service" "aoc" {
  # don't do lb if the sample app is not callable
  count            = var.sample_app_callable ? 1 : 0
  name             = "aocservice-${module.common.testing_id}"
  cluster          = aws_ecs_cluster.ecscluster.id
  task_definition  = "${aws_ecs_task_definition.aoc[0].family}:1"
  desired_count    = 1
  launch_type      = var.ecs_launch_type
  platform_version = var.ecs_launch_type == "FARGATE" ? "1.4.0" : null

  load_balancer {
    target_group_arn = aws_lb_target_group.aoc_lb_tg[0].arn
    container_name   = module.common.sample_app_container_name
    container_port   = module.common.sample_app_listen_address_port
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.mocked_server_lb_tg[0].arn
    container_name   = "mocked-server"
    container_port   = module.common.mocked_server_http_port
  }

  network_configuration {
    subnets         = module.basic_components.aoc_private_subnet_ids
    security_groups = [module.basic_components.aoc_security_group_id]
  }

  depends_on = [null_resource.scale_down_asg, null_resource.check_patch, aws_ecs_task_definition.aoc]
}

# remove lb since there's no callable sample app, some test cases will drop in here, for example, ecsmetadata receiver test
resource "aws_ecs_service" "aoc_without_sample_app" {
  count            = !var.sample_app_callable && var.ecs_taskdef_network_mode == "awsvpc" ? 1 : 0
  name             = "aocservice-${module.common.testing_id}"
  cluster          = aws_ecs_cluster.ecscluster.id
  task_definition  = "${aws_ecs_task_definition.aoc[0].family}:1"
  desired_count    = 1
  launch_type      = var.ecs_launch_type
  platform_version = var.ecs_launch_type == "FARGATE" ? "1.4.0" : null

  network_configuration {
    subnets         = module.basic_components.aoc_private_subnet_ids
    security_groups = [module.basic_components.aoc_security_group_id]
  }
  depends_on = [null_resource.check_patch, aws_ecs_task_definition.aoc]
}

resource "aws_ecs_service" "aoc_without_sample_app_for_bridge" {
  count           = !var.sample_app_callable && var.ecs_taskdef_network_mode == "bridge" ? 1 : 0
  name            = "aocservice-${module.common.testing_id}"
  cluster         = aws_ecs_cluster.ecscluster.id
  task_definition = "${aws_ecs_task_definition.aoc_bridge[0].family}:1"
  desired_count   = 1
  launch_type     = "EC2"
  depends_on      = [null_resource.scale_down_asg, null_resource.check_patch, aws_ecs_task_definition.aoc_bridge]
}

##########################################
# Validation
##########################################
module "validator" {
  count  = var.sample_app_callable ? 1 : 0
  source = "../validation"

  validation_config            = var.validation_config
  region                       = var.region
  testing_id                   = module.common.testing_id
  metric_namespace             = "${module.common.otel_service_namespace}/${module.common.otel_service_name}"
  sample_app_endpoint          = "http://${aws_lb.aoc_lb[0].dns_name}:${module.common.sample_app_lb_port}"
  mocked_server_validating_url = "http://${aws_lb.mocked_server_lb[0].dns_name}:${module.common.mocked_server_lb_port}/check-data"
  cortex_instance_endpoint     = var.cortex_instance_endpoint
  #language = "javascript"

  account_id = data.aws_caller_identity.current.account_id

  ecs_context_json = jsonencode({
    ecsClusterName : aws_ecs_cluster.ecscluster.name
    ecsClusterArn : aws_ecs_cluster.ecscluster.arn
    ecsTaskDefArn : aws_ecs_task_definition.aoc[0].arn
    ecsTaskDefFamily : aws_ecs_task_definition.aoc[0].family
    ecsTaskDefVersion : aws_ecs_task_definition.aoc[0].revision
    ecsLaunchType : aws_ecs_service.aoc[0].launch_type
  })

  depends_on = [aws_ecs_service.aoc]
}

module "validator_without_sample_app" {
  count  = !var.sample_app_callable && var.ecs_taskdef_network_mode == "awsvpc" ? 1 : 0
  source = "../validation"

  validation_config            = var.validation_config
  region                       = var.region
  testing_id                   = module.common.testing_id
  metric_namespace             = "${module.common.otel_service_namespace}/${module.common.otel_service_name}"
  mocked_server_validating_url = var.disable_mocked_server ? "" : "http://${aws_lb.mocked_server_lb[0].dns_name}:${module.common.mocked_server_lb_port}/check-data"

  ecs_context_json = jsonencode({
    ecsClusterName : aws_ecs_cluster.ecscluster.name
    ecsTaskDefArn : aws_ecs_task_definition.aoc[0].arn
    ecsTaskDefFamily : aws_ecs_task_definition.aoc[0].family
    ecsTaskDefVersion : aws_ecs_task_definition.aoc[0].revision
  })

  cloudwatch_context_json = data.template_file.cloudwatch_context.rendered

  depends_on = [aws_ecs_service.aoc_without_sample_app, aws_ecs_service.extra_apps]
}

module "validator_without_sample_app_for_bridge" {
  count  = !var.sample_app_callable && var.ecs_taskdef_network_mode == "bridge" ? 1 : 0
  source = "../validation"

  validation_config            = var.validation_config
  region                       = var.region
  testing_id                   = module.common.testing_id
  metric_namespace             = "${module.common.otel_service_namespace}/${module.common.otel_service_name}"
  mocked_server_validating_url = var.disable_mocked_server ? "" : "http://${aws_lb.mocked_server_lb[0].dns_name}:${module.common.mocked_server_lb_port}/check-data"
  cloudwatch_context_json      = data.template_file.cloudwatch_context.rendered

  ecs_context_json = jsonencode({
    ecsClusterName : aws_ecs_cluster.ecscluster.name
    ecsTaskDefArn : aws_ecs_task_definition.aoc_bridge[0].arn
    ecsTaskDefFamily : aws_ecs_task_definition.aoc_bridge[0].family
    ecsTaskDefVersion : aws_ecs_task_definition.aoc_bridge[0].revision
  })

  depends_on = [
  aws_ecs_service.aoc_without_sample_app_for_bridge]
}

data "template_file" "cloudwatch_context" {
  # default is just empty json, each test case can set its own override under its own folder.
  # See containerinsight_ecs_prometheus as example.
  template = file(local.cloudwatch_context_path)
  vars = {
    testing_id   = module.common.testing_id
    cluster_name = aws_ecs_cluster.ecscluster.name
  }
}
