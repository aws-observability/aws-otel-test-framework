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

  aoc_version = var.aoc_version
  commit_id   = var.commit_id
  testcase    = var.testcase
  testing_ami = var.testing_ami

  sample_app_mode       = var.sample_app_mode
  soaking_sample_app    = var.soaking_sample_app
  soaking_data_rate     = var.data_rate
  soaking_data_type     = var.soaking_data_type
  soaking_data_mode     = var.soaking_data_mode
  sidecar_instance_type = var.sidecar_instance_type

  cortex_instance_endpoint = var.cortex_instance_endpoint

  install_package_source     = var.install_package_source
  install_package_local_path = var.install_package_local_path

  ssh_key_name          = var.ssh_key_name
  sshkey_s3_bucket      = var.sshkey_s3_bucket
  sshkey_s3_private_key = var.sshkey_s3_private_key

  soaking_metric_namespace = var.performance_metric_namespace

  debug = var.debug

  testing_type = "perf"
}

locals {
  validation_config_file = "performance_validation.yml"
  ami_family             = module.ec2_setup.ami_family
  otconfig               = yamldecode(module.ec2_setup.otconfig_content)
  ot_components          = lookup(local.otconfig["service"]["pipelines"], "${var.soaking_data_mode}s", {})
  ot_receivers           = lookup(local.ot_components, "receivers", [])
  ot_processors          = lookup(local.ot_components, "processors", [])
  ot_exporters           = lookup(local.ot_components, "exporters", [])
}

data "template_file" "validation_config" {
  template = file("../templates/defaults/performance_validation.tpl")

  vars = {
    cpuMetricName    = local.ami_family["soaking_cpu_metric_name"]
    memoryMetricName = local.ami_family["soaking_mem_metric_name"]
    collectionPeriod = var.collection_period
    dataType         = var.soaking_data_type
    dataMode         = var.soaking_data_mode
    dataRate         = var.data_rate
    otReceivers      = join(", ", local.ot_receivers)
    otProcessors     = join(", ", local.ot_processors)
    otExporters      = join(", ", local.ot_exporters)
    testcase         = split("/", var.testcase)[2]
    commitId         = module.ec2_setup.commit_id
    instanceId       = module.ec2_setup.collector_instance_id
    instanceType     = module.ec2_setup.collector_instance_type
    launchDate       = module.ec2_setup.launch_date
    exe              = "aws-otel-collector"
    processName      = local.ami_family["soaking_process_name"]
    testingAmi       = var.testing_ami
    negativeSoaking  = module.ec2_setup.negative_soaking
  }
}

resource "local_file" "validation_config_file" {
  content = data.template_file.validation_config.rendered

  filename = "../../validator/src/main/resources/validations/${local.validation_config_file}"

  depends_on = [data.template_file.validation_config]
}

##########################################
# Validation
##########################################
resource "time_sleep" "wait_until_metrics_collected" {
  create_duration = "${var.collection_period}m"
  depends_on      = [module.ec2_setup]
}

module "validator" {
  source = "../validation"

  validation_config = local.validation_config_file
  region            = var.region
  testing_id        = module.ec2_setup.testing_id
  metric_namespace  = var.performance_metric_namespace

  depends_on = [time_sleep.wait_until_metrics_collected]
}
