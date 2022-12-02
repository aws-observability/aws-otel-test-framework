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
  region = var.region
}

module "ec2_setup" {
  source = "../ec2_setup"

  testcase    = var.testcase
  testing_ami = var.testing_ami
  aoc_version = var.aoc_version
  region      = var.region

  sample_app_mode    = var.sample_app_mode
  soaking_sample_app = var.soaking_sample_app
  soaking_data_mode  = var.soaking_data_mode
  soaking_data_rate  = var.soaking_data_rate
  soaking_data_type  = var.soaking_data_type

  cortex_instance_endpoint = var.cortex_instance_endpoint

  install_package_source     = var.install_package_source
  install_package_local_path = var.install_package_local_path

  commit_id = var.commit_id

  ssh_key_name          = var.ssh_key_name
  sshkey_s3_bucket      = var.sshkey_s3_bucket
  sshkey_s3_private_key = var.sshkey_s3_private_key

  debug            = var.debug
  negative_soaking = var.negative_soaking

  testing_type = "soaking"

  patch = true
}

locals {
  ami_family      = module.ec2_setup.ami_family
  instance_type   = local.ami_family["instance_type"]
  login_user      = local.ami_family["login_user"]
  connection_type = local.ami_family["connection_type"]
}

# create alarm
## create cloudwatch alarm base on the metrics emitted by cwagent
# wait 2 minute for the metrics to be available on cloudwatch
resource "time_sleep" "wait_until_metric_appear" {
  create_duration = "120s"
  depends_on      = [module.ec2_setup]
}

# cpu alarm
resource "aws_cloudwatch_metric_alarm" "cpu_alarm" {
  depends_on          = [time_sleep.wait_until_metric_appear]
  alarm_name          = "otel-soaking-cpu-alarm-${module.ec2_setup.testing_id}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 5
  threshold           = "200"

  metric_query {
    id          = "cpu"
    return_data = true

    metric {
      metric_name = local.ami_family["soaking_cpu_metric_name"]
      namespace   = var.soaking_metric_namespace
      period      = 60
      stat        = "Average"
      unit        = "Percent"

      # use this dimension to identify each test
      dimensions = {
        InstanceId       = module.ec2_setup.collector_instance_id
        exe              = "aws-otel-collector"
        process_name     = local.ami_family["soaking_process_name"]
        testcase         = split("/", var.testcase)[2]
        launch_date      = module.ec2_setup.launch_date
        commit_id        = module.ec2_setup.commit_id
        negative_soaking = module.ec2_setup.negative_soaking
        data_rate        = "${var.soaking_data_mode}-${var.soaking_data_rate}"
        instance_type    = module.ec2_setup.collector_instance_type
        testing_ami      = var.testing_ami
      }
    }
  }
}

# mem alarm
resource "aws_cloudwatch_metric_alarm" "mem_alarm" {
  depends_on          = [time_sleep.wait_until_metric_appear]
  alarm_name          = "otel-soaking-mem-alarm-${module.ec2_setup.testing_id}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 5
  threshold           = "400000000"

  metric_query {
    id          = "mem"
    return_data = true

    metric {
      metric_name = local.ami_family["soaking_mem_metric_name"]
      namespace   = var.soaking_metric_namespace
      period      = 60
      stat        = "Average"

      # use this dimension to identify each test
      dimensions = {
        InstanceId       = module.ec2_setup.collector_instance_id
        exe              = "aws-otel-collector"
        process_name     = local.ami_family["soaking_process_name"]
        testcase         = split("/", var.testcase)[2]
        launch_date      = module.ec2_setup.launch_date
        commit_id        = module.ec2_setup.commit_id
        negative_soaking = module.ec2_setup.negative_soaking
        data_rate        = "${var.soaking_data_mode}-${var.soaking_data_rate}"
        instance_type    = module.ec2_setup.collector_instance_type
        testing_ami      = var.testing_ami
      }
    }
  }
}

# incoming packets alarm on the aoc instance to ensure the pressure
resource "aws_cloudwatch_metric_alarm" "incoming_bytes" {
  depends_on          = [time_sleep.wait_until_metric_appear]
  alarm_name          = "otel-soaking-incoming-bytes-alarm-${module.ec2_setup.testing_id}"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 5
  threshold           = "10000" # bytes

  metric_query {
    id          = "incoming_bytes"
    return_data = true

    metric {
      metric_name = "NetworkIn"
      namespace   = "AWS/EC2"
      period      = 60
      stat        = "Average"

      # use this dimension to identify each test
      dimensions = {
        InstanceId = module.ec2_setup.collector_instance_id
      }
    }
  }
}

##########################################
# Validation
##########################################
resource "time_sleep" "wait_until_metric_is_sufficient" {
  create_duration = "600s"
  depends_on      = [aws_cloudwatch_metric_alarm.cpu_alarm, aws_cloudwatch_metric_alarm.mem_alarm, aws_cloudwatch_metric_alarm.incoming_bytes]
}

module "validator" {
  source = "../validation"

  validation_config      = "alarm-pulling-validation.yml"
  region                 = var.region
  testing_id             = module.ec2_setup.testing_id
  testcase               = split("/", var.testcase)[2]
  cpu_alarm              = aws_cloudwatch_metric_alarm.cpu_alarm.alarm_name
  mem_alarm              = aws_cloudwatch_metric_alarm.mem_alarm.alarm_name
  incoming_packets_alarm = aws_cloudwatch_metric_alarm.incoming_bytes.alarm_name


  depends_on = [time_sleep.wait_until_metric_is_sufficient]
}


