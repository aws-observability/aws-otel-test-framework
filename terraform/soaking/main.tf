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
provider "aws" {
  region  = var.region
}

locals {
  launch_date = formatdate("YYYY-MM-DD", timestamp())
}
# launch ec2
module "ec2_setup" {
  source = "../ec2"

  ami_family = var.ami_family
  amis = var.amis
  testing_ami = var.testing_ami
  aoc_version = var.aoc_version
  region = var.region
  testcase = var.testcase
  sample_app_image = var.soaking_data_emitter_image
  skip_validation = true

  # soaking test config
  soaking_compose_file = "../templates/defaults/soaking_docker_compose.tpl"
  soaking_data_mode = var.soaking_data_mode
  soaking_data_rate = var.soaking_data_rate
  soaking_data_type = var.soaking_data_type

  # negative soaking
  mock_endpoint = var.negative_soaking ? "http://127.0.0.2" : "mocked-server/put-data"

  # install cwagent
  install_cwagent = true

  # use our own ssh key name
  ssh_key_name = "aoc-ssh-key-2020-07-22"
  sshkey_s3_bucket = "aoc-ssh-key"
  sshkey_s3_private_key = "aoc-ssh-key-2020-07-22.pem"

  # additional dimension
  commit_id = var.commit_id
  launch_date = local.launch_date
  negative_soaking = var.negative_soaking
}

locals {
  selected_ami = var.amis[var.testing_ami]
  ami_family = var.ami_family[local.selected_ami["family"]]
  instance_type = local.ami_family["instance_type"]
  login_user = local.ami_family["login_user"]
  connection_type = local.ami_family["connection_type"]
}




# create alarm
## create cloudwatch alarm base on the metrics emitted by cwagent
# wait 2 minute for the metrics to be available on cloudwatch
resource "time_sleep" "wait_until_metric_appear" {
  create_duration = "120s"
  depends_on = [module.ec2_setup]
}

# cpu alarm
resource "aws_cloudwatch_metric_alarm" "cpu_alarm" {
  depends_on = [time_sleep.wait_until_metric_appear]
  alarm_name = "otel-soaking-cpu-alarm-${module.ec2_setup.testing_id}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = 5
  threshold = "200"

  metric_query {
    id = "cpu"
    return_data = true

    metric {
      metric_name = local.ami_family["soaking_cpu_metric_name"]
      namespace = var.soaking_metric_namespace
      period = 60
      stat = "Average"
      unit = "Percent"

      # use this dimension to identify each test
      dimensions = {
        InstanceId = module.ec2_setup.collector_instance_id
        exe = "aws-otel-collector"
        process_name = local.ami_family["soaking_process_name"]
        testcase = split("/", var.testcase)[2]
        testing_ami = var.testing_ami
        launch_date = local.launch_date
        commit_id = var.commit_id
        negative_soaking = var.negative_soaking
      }
    }
  }
}

# mem alarm
resource "aws_cloudwatch_metric_alarm" "mem_alarm" {
  depends_on = [time_sleep.wait_until_metric_appear]
  alarm_name = "otel-soaking-mem-alarm-${module.ec2_setup.testing_id}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = 5
  threshold = "4000000000"

  metric_query {
    id = "mem"
    return_data = true

    metric {
      metric_name = local.ami_family["soaking_mem_metric_name"]
      namespace = var.soaking_metric_namespace
      period = 60
      stat = "Average"

      # use this dimension to identify each test
      dimensions = {
        InstanceId = module.ec2_setup.collector_instance_id
        exe = "aws-otel-collector"
        process_name = local.ami_family["soaking_process_name"]
        testcase = split("/", var.testcase)[2]
        testing_ami = var.testing_ami
        launch_date = local.launch_date
        commit_id = var.commit_id
        negative_soaking = var.negative_soaking
      }
    }
  }
}

# incoming packets alarm on the aoc instance to ensure the pressure
resource "aws_cloudwatch_metric_alarm" "incoming_packets" {
  depends_on = [time_sleep.wait_until_metric_appear]
  alarm_name = "otel-soaking-incoming-packets-alarm-${module.ec2_setup.testing_id}"
  comparison_operator = "LessThanThreshold"
  evaluation_periods = 5
  threshold = "130"

  metric_query {
    id = "incoming_bytes"
    return_data = true

    metric {
      metric_name = "NetworkPacketsIn"
      namespace = "AWS/EC2"
      period = 60
      stat = "Average"

      # use this dimension to identify each test
      dimensions = {
        InstanceId = module.ec2_setup.sample_app_instance_id
      }
    }
  }
}

##########################################
# Validation
##########################################
resource "time_sleep" "wait_until_metric_is_sufficient" {
  create_duration = "600s"
  depends_on = [aws_cloudwatch_metric_alarm.cpu_alarm, aws_cloudwatch_metric_alarm.mem_alarm, aws_cloudwatch_metric_alarm.incoming_packets]
}

module "validator" {
  source = "../validation"

  validation_config = "alarm-pulling-validation.yml"
  region = var.region
  testing_id = module.ec2_setup.testing_id
  cpu_alarm = aws_cloudwatch_metric_alarm.cpu_alarm.alarm_name
  mem_alarm = aws_cloudwatch_metric_alarm.mem_alarm.alarm_name
  incoming_packets_alarm = aws_cloudwatch_metric_alarm.incoming_packets.alarm_name

  aws_access_key_id = var.aws_access_key_id
  aws_secret_access_key = var.aws_secret_access_key

  depends_on = [time_sleep.wait_until_metric_is_sufficient]
}

output "collector_instance" {
  value = module.ec2_setup.collector_instance_public_ip
}

output "sample_app_instance" {
  value = module.ec2_setup.sample_app_instance_public_ip
}

