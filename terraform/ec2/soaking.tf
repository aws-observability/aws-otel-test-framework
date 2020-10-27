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

## install cwagent on the instance to collect metric from otel-collector
data "template_file" "cwagent_config" {
  count = var.soaking ? 1 : 0
  template = file(local.ami_family["soaking_cwagent_config"])

  vars = {
    soaking_metric_namespace = var.soaking_metric_namespace
  }
}

resource "null_resource" "install_cwagent" {
  count = var.soaking ? 1 : 0

  // copy cwagent config to the instance
  provisioner "file" {
    content = data.template_file.cwagent_config[0].rendered
    destination = local.ami_family["soaking_cwagent_config_destination"]

    connection {
      type = local.connection_type
      user = local.login_user
      private_key = local.connection_type == "ssh" ? data.aws_s3_bucket_object.ssh_private_key.body : null
      password = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, data.aws_s3_bucket_object.ssh_private_key.body) : null
      host = aws_instance.aoc.public_ip
    }
  }

  provisioner "remote-exec" {
    inline = [
      local.ami_family["cwagent_download_command"],
      local.ami_family["cwagent_start_command"]
    ]

    connection {
      type = local.connection_type
      user = local.login_user
      private_key = local.connection_type == "ssh" ? data.aws_s3_bucket_object.ssh_private_key.body : null
      password = local.connection_type == "winrm" ? rsadecrypt(aws_instance.aoc.password_data, data.aws_s3_bucket_object.ssh_private_key.body) : null
      host = aws_instance.aoc.public_ip
    }
  }
}

## create cloudwatch alarm base on the metrics emitted by cwagent
# wait 2 minute for the metrics to be available on cloudwatch
resource "time_sleep" "wait_2_minutes" {
  depends_on = [null_resource.install_cwagent[0]]

  create_duration = "120s"
}
# cpu alarm
resource "aws_cloudwatch_metric_alarm" "cpu_alarm" {
  count = var.soaking ? 1 : 0
  depends_on = [time_sleep.wait_2_minutes]
  alarm_name = "otel-soaking-cpu-alarm-${module.common.testing_id}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods = 2
  threshold = "50"

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
        InstanceId = aws_instance.aoc.id
      }
    }
  }
}

# soaking alarm pulling
resource "null_resource" "bake_alarms" {
  depends_on = [aws_cloudwatch_metric_alarm.cpu_alarm]
  count = var.soaking ? 1 : 0
  provisioner "local-exec" {
    command = "${module.common.validator_path} --args='-c ${var.validation_config} -t ${module.common.testing_id} --region ${var.region} --alarm-names ${aws_cloudwatch_metric_alarm.cpu_alarm[0].alarm_name}'"
    working_dir = "../../"
  }
}
