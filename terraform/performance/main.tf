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

module "ec2_setup" {
  source = "../ec2_setup"

  testing_ami = var.testing_ami
  soaking_data_rate = var.data_rate
  soaking_data_type = var.data_type
  install_package_source = var.install_package_source
  install_package_local_path = var.install_package_local_path

  ssh_key_name = var.ssh_key_name
  sshkey_s3_bucket = var.sshkey_s3_bucket
  sshkey_s3_private_key = var.sshkey_s3_private_key

  soaking_metric_namespace = "AWSOtelCollector/PerfTest"

  debug = var.debug
}

locals{
  validation_config_file = "performance_validation.yml"
  ami_family = module.ec2_setup.ami_family
}

data "template_file" "validation_config" {
  template = file("../templates/defaults/performance-validation.tpl")

  vars = {
    cpuMetricName = local.ami_family["soaking_cpu_metric_name"]
    memoryMetricName = local.ami_family["soaking_mem_metric_name"]
    testcase = split("/", var.testcase)[2]
    commitId = module.ec2_setup.commit_id
    instanceType = module.ec2_setup.collector_instance_type
    dataType = var.data_type
    dataRate = var.data_rate
    collectionPeriod = var.collection_period
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
  create_duration = "5s"
  depends_on = [module.ec2_setup]
}

module "validator" {
  source = "../validation"

  validation_config = local.validation_config_file
  region = var.region
  testing_id = module.ec2_setup.testing_id
  metric_namespace = var.soaking_metric_namespace

  aws_access_key_id = var.aws_access_key_id
  aws_secret_access_key = var.aws_secret_access_key

  depends_on = [time_sleep.wait_until_metrics_collected]
}
