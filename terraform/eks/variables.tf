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

variable "eks_cluster_name" {
  default = "aws-otel-testing-framework-eks-2"
}

variable "mock_endpoint" {
  default = "localhost/put-data"
}

// The sample_app_image_repo is the image repo for traffic generator applications used by appmesh and jmx module.
// To rebuild images, please follow the documents:
// appmesh: https://github.com/aws/aws-app-mesh-examples/tree/master/walkthroughs/howto-k8s-http-headers
// jmx: https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights-Prometheus-Sample-Workloads-javajmx.html
// Don't forget to re-tag the images to ${sample_app_image_repo}:(feapp|colorapp|tomcatapp) before pushing to remote.
variable "sample_app_image_repo" {
  default = "611364707713.dkr.ecr.us-west-2.amazonaws.com/otel-test/container-insight-samples"
}

// aoc_base_scenario refers to the base scenario that the aoc is used for.
// options: oltp, prometheus, infra
variable "aoc_base_scenario" {
  default = "oltp"
}

// aoc_deploy_mode refers to the mode to deploy the Collector CR. This is only used when we test the Operator.
// options: deployment, daemonset, statefulset, sidecar
variable "aoc_deploy_mode" {
  default = "deployment"
}

variable "deployment_type" {
  default     = "ec2"
  description = "ec2 or fargate"
}

variable "rollup" {
  type    = bool
  default = true
}

variable "fargate_sample_app_lb_port" {
  default = "80"
}