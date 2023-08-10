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

locals {
  eks_pod_config_path = fileexists("${var.testcase}/eks_pod_config.tpl") ? "${var.testcase}/eks_pod_config.tpl" : module.common.default_eks_pod_config_path
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

data "aws_caller_identity" "current" {
}

# get eks cluster by name
data "aws_eks_cluster" "testing_cluster" {
  name = var.eks_cluster_name
}
data "aws_eks_cluster_auth" "testing_cluster" {
  name = var.eks_cluster_name
}

# set up kubectl
provider "kubernetes" {
  host                   = data.aws_eks_cluster.testing_cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.testing_cluster.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.testing_cluster.token
}

provider "kubectl" {
  // Note: copy from eks module. Please avoid use shorted-lived tokens when running locally.
  // For more information: https://registry.terraform.io/providers/hashicorp/kubernetes/latest/docs#exec-plugins
  host                   = data.aws_eks_cluster.testing_cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.testing_cluster.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.testing_cluster.token
  load_config_file       = false
}

data "template_file" "kubeconfig_file" {
  template = file("./kubeconfig.tpl")
  vars = {
    CA_DATA : data.aws_eks_cluster.testing_cluster.certificate_authority[0].data
    SERVER_ENDPOINT : data.aws_eks_cluster.testing_cluster.endpoint
    TOKEN = data.aws_eks_cluster_auth.testing_cluster.token
  }
}

resource "local_file" "kubeconfig" {
  filename = "kubeconfig"
  content  = data.template_file.kubeconfig_file.rendered
}

provider "helm" {
  kubernetes {
    host                   = data.aws_eks_cluster.testing_cluster.endpoint
    cluster_ca_certificate = base64decode(data.aws_eks_cluster.testing_cluster.certificate_authority[0].data)
    token                  = data.aws_eks_cluster_auth.testing_cluster.token
  }
}

# create a unique namespace for each run
resource "kubernetes_namespace" "aoc_ns" {
  metadata {
    name = "aoc-ns-${module.common.testing_id}"
  }
}

# create a unique fargate namespace for each run
resource "kubernetes_namespace" "aoc_fargate_ns" {
  metadata {
    name = "aoc-fargate-ns-${module.common.testing_id}"
  }
}

resource "kubernetes_service_account" "aoc-role" {
  metadata {
    name      = "aoc-role-${module.common.testing_id}"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
    annotations = {
      "eks.amazonaws.com/role-arn" : module.iam_assumable_role_admin.iam_role_arn
    }
  }

  depends_on                      = [module.iam_assumable_role_admin]
  automount_service_account_token = true
}

resource "kubernetes_service_account" "aoc-fargate-role" {
  count = var.deployment_type == "fargate" ? 1 : 0
  metadata {
    name      = "aoc-fargate-role-${module.common.testing_id}"
    namespace = kubernetes_namespace.aoc_fargate_ns.metadata[0].name
    annotations = {
      "eks.amazonaws.com/role-arn" : module.iam_assumable_role_admin.iam_role_arn
    }
  }

  automount_service_account_token = true
  depends_on                      = [module.iam_assumable_role_admin]
}

module "iam_assumable_role_admin" {
  create_role = true

  role_name = "aoc-eks-assume-role-${module.common.testing_id}"

  provider_url = trimprefix(data.aws_eks_cluster.testing_cluster.identity[0].oidc[0].issuer, "https://")

  role_policy_arns = [
    "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy",
    "arn:aws:iam::aws:policy/AWSXrayFullAccess",
    "arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess",
    "arn:aws:iam::aws:policy/AmazonPrometheusRemoteWriteAccess",
    "arn:aws:iam::aws:policy/AWSAppMeshEnvoyAccess",
  ]
  source  = "terraform-aws-modules/iam/aws//modules/iam-assumable-role-with-oidc"
  version = "4.7.0"
}

resource "kubernetes_cluster_role_binding" "aoc-role-binding" {
  count = 1
  metadata {
    name = "aoc-role-binding-${module.common.testing_id}"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "cluster-admin"
  }
  subject {
    kind      = "ServiceAccount"
    name      = var.deployment_type == "fargate" ? "aoc-fargate-role-${module.common.testing_id}" : "aoc-role-${module.common.testing_id}"
    namespace = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
  }
}

resource "kubernetes_service_account" "aoc-agent-role" {
  count = 1
  metadata {
    name      = "aoc-agent-${module.common.testing_id}"
    namespace = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
    annotations = {
      "eks.amazonaws.com/role-arn" : module.iam_assumable_role_admin.iam_role_arn
    }
  }
  depends_on = [module.iam_assumable_role_admin]

  automount_service_account_token = true
}

module "adot_operator" {
  count  = replace(var.testcase, "_adot_operator", "") == var.testcase ? 0 : 1
  source = "./adot-operator"

  testing_id          = module.common.testing_id
  kubeconfig          = local_file.kubeconfig.filename
  operator_repository = var.operator_repository
  operator_tag        = var.operator_tag
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
  sample_app_endpoint = (length(kubernetes_ingress.app) > 0 && var.deployment_type == "fargate" ? "http://${kubernetes_ingress.app[0].status[0].load_balancer[0].ingress[0].hostname}:${var.fargate_sample_app_lb_port}" : (
    length(kubernetes_service.sample_app_service) > 0 ? "http://${kubernetes_service.sample_app_service[0].status[0].load_balancer[0].ingress[0].hostname}:${module.common.sample_app_lb_port}" : ""
    )
  )
  mocked_server_validating_url = length(kubernetes_service.mocked_server_service) > 0 ? "http://${kubernetes_service.mocked_server_service[0].status[0].load_balancer[0].ingress[0].hostname}/check-data" : ""
  cloudwatch_context_json = var.aoc_base_scenario == "prometheus" ? jsonencode({
    clusterName : var.eks_cluster_name
    #    appMesh : {
    #      namespace : module.demo_appmesh.0.metric_dimension_namespace
    #      job : "kubernetes-pod-appmesh-envoy"
    #    }
    nginx : {
      namespace : module.demo_nginx.0.metric_dimension_namespace
      job : "kubernetes-service-endpoints"
    }
    jmx : {
      namespace : module.demo_jmx.0.metric_dimension_namespace
      job : "kubernetes-pod-jmx"
    }
    memcached : {
      namespace : module.demo_memcached.0.metric_dimension_namespace
      job : "kubernetes-service-endpoints"
    }
    haproxy : {
      namespace : module.demo_haproxy.0.metric_dimension_namespace
      job : "kubernetes-service-endpoints"
    }
    }) : jsonencode({
    clusterName : var.eks_cluster_name
    ignoreEmptyDimSet : var.ignore_empty_dim_set
  })
  cortex_instance_endpoint = var.cortex_instance_endpoint
  rollup                   = var.rollup
  kubecfg_file_path        = abspath("./${local_file.kubeconfig.filename}")
  k8s_deployment_name      = var.aoc_base_scenario == "otlp" ? module.aoc_otlp[0].sample_app_deployment_name : ""
  k8s_namespace            = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name

  depends_on = [
    module.aoc_otlp,
    module.adot_operator,
    kubectl_manifest.logs_sample_fargate_deploy,
    null_resource.prom_base_ready_check,
    kubernetes_deployment.aoc_deployment,
  ]
}

