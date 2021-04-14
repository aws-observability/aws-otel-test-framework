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

# creating a eks cluster takes around 10 minutes typically.
# so in the eks/k8s test, we need tester to provide the cluster instead of creating it in terraform
# so that we can shorten the execution time

terraform {
  required_version = ">= 0.13"

  required_providers {
    kubectl = {
      source  = "gavinbunney/kubectl"
      version = ">= 1.7.0"
    }
  }
}

module "common" {
  source = "../common"

  aoc_image_repo = var.aoc_image_repo
  aoc_version    = var.aoc_version
}

# region
provider "aws" {
  region = var.region
}

# get eks cluster by name
data "aws_eks_cluster" "testing_cluster" {
  name = var.eks_cluster_name
}

data "aws_eks_cluster_auth" "testing_cluster" {
  name = var.eks_cluster_name
}


provider "kubectl" {
  // Note: copy from eks module. Please avoid use shorted-lived tokens when running locally.
  // For more information: https://registry.terraform.io/providers/hashicorp/kubernetes/latest/docs#exec-plugins
  host                   = data.aws_eks_cluster.testing_cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.testing_cluster.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.testing_cluster.token
  load_config_file       = false
}

data "template_file" "namespace_file" {
  template = file("../testcases/containerinsight_eks/namespace.tpl")
  vars = {
    NAMESPACE = "aoc-ns-${module.common.testing_id}"
  }
}

data "template_file" "service_account_file" {
  template = file("../testcases/containerinsight_eks/service_account.tpl")
  vars = {
    NAMESPACE = "aoc-ns-${module.common.testing_id}"
  }
}

data "template_file" "cluster_role_file" {
  template = file("../testcases/containerinsight_eks/cluster_role.tpl")
  vars = {
    NAMESPACE = "aoc-ns-${module.common.testing_id}"
  }
}

data "template_file" "cluster_role_binding_file" {
  template = file("../testcases/containerinsight_eks/cluster_role_binding.tpl")
  vars = {
    NAMESPACE = "aoc-ns-${module.common.testing_id}"
  }
}

data "template_file" "config_map_file" {
  template = file("../testcases/containerinsight_eks/config_map.tpl")
  vars = {
    NAMESPACE = "aoc-ns-${module.common.testing_id}"
  }
}

data "template_file" "daemonset_file" {
  template = file("../testcases/containerinsight_eks/daemonset.tpl")
  vars = {
    NAMESPACE = "aoc-ns-${module.common.testing_id}"
    OTELIMAGE = module.common.aoc_image
    REGION    = var.region
  }
}

resource "kubectl_manifest" "namespace" {
  yaml_body = data.template_file.namespace_file.rendered
}

resource "kubectl_manifest" "service_account" {
  yaml_body = data.template_file.service_account_file.rendered
  depends_on = [
    kubectl_manifest.namespace
  ]
}

resource "kubectl_manifest" "cluster_role" {
  yaml_body = data.template_file.cluster_role_file.rendered
}

resource "kubectl_manifest" "cluster_role_binding" {
  yaml_body = data.template_file.cluster_role_binding_file.rendered
  depends_on = [
    kubectl_manifest.cluster_role
  ]
}

resource "kubectl_manifest" "config_map" {
  yaml_body = data.template_file.config_map_file.rendered
  depends_on = [
    kubectl_manifest.namespace
  ]
}

resource "kubectl_manifest" "daemonset" {
  yaml_body = data.template_file.daemonset_file.rendered
  depends_on = [
    kubectl_manifest.config_map,
    kubectl_manifest.service_account
  ]
}


##########################################
# Validation
##########################################
module "validator" {
  source = "../validation"

  validation_config = var.validation_config
  region            = var.region
  testing_id        = module.common.testing_id
  metric_namespace  = "${module.common.otel_service_namespace}/${module.common.otel_service_name}"
  cloudwatch_context_json = jsonencode({
    clusterName : var.eks_cluster_name
  })

  aws_access_key_id     = var.aws_access_key_id
  aws_secret_access_key = var.aws_secret_access_key
}