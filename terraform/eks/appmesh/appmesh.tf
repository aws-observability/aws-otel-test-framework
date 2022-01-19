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

variable "provider_url" {
  type    = string
  default = ""
}

variable "sample_app_image_repo" {
  type    = string
  default = null
}

variable "testing_id" {
  type    = string
  default = ""
}

variable "region" {
  type    = string
  default = "us-west-2"
}

variable "kubeconfig" {
  type    = string
  default = "kubeconfig"
}

output "metric_dimension_namespace" {
  value = kubernetes_namespace.traffic_ns.metadata[0].name
}

data "aws_caller_identity" "current" {}

locals {
  mesh_name      = "howto-k8s-http-headers-${var.testing_id}"
  aws_account_id = data.aws_caller_identity.current.account_id
}

data "aws_iam_policy" "appmesh_policy" {
  arn = "arn:aws:iam::${local.aws_account_id}:policy/AWSAppMeshK8sControllerIAMPolicy"
}

module "iam_assumable_role_with_oidc" {
  source       = "terraform-aws-modules/iam/aws//modules/iam-assumable-role-with-oidc"
  version      = "~> 3.0"
  provider_url = var.provider_url

  create_role = true
  role_name   = "AppMeshControllerRole-${var.testing_id}"
  role_policy_arns = [
  data.aws_iam_policy.appmesh_policy.arn]

  oidc_fully_qualified_subjects = [
  "system:serviceaccount:${kubernetes_namespace.appmesh_ns.metadata[0].name}:appmesh-controller"]
  number_of_role_policy_arns = 1
}

resource "kubernetes_namespace" "appmesh_ns" {
  metadata {
    name = "appmesh-system-${var.testing_id}"
  }
}

resource "kubernetes_service_account" "appmesh_sa" {
  metadata {
    name      = "appmesh-controller"
    namespace = kubernetes_namespace.appmesh_ns.metadata[0].name
    annotations = {
      "eks.amazonaws.com/role-arn" = module.iam_assumable_role_with_oidc.this_iam_role_arn
    }
  }
  automount_service_account_token = true
}

resource "helm_release" "eks" {
  name      = "eks-${var.testing_id}"
  namespace = kubernetes_namespace.appmesh_ns.metadata[0].name

  repository = "https://aws.github.io/eks-charts"
  chart      = "appmesh-controller"
  version    = "1.4"

  set {
    name  = "serviceAccount.create"
    value = "false"
  }

  set {
    name  = "serviceAccount.name"
    value = kubernetes_service_account.appmesh_sa.metadata[0].name
  }

  set {
    name  = "region"
    value = var.region
  }

  provisioner "local-exec" {
    command = "kubectl --kubeconfig=${var.kubeconfig} apply -k \"github.com/aws/eks-charts/stable/appmesh-controller/crds?ref=v0.0.47\""
  }

  wait = true
  depends_on = [
  module.iam_assumable_role_with_oidc]
}

resource "kubernetes_namespace" "traffic_ns" {
  metadata {
    name = "howto-k8s-http-headers-${var.testing_id}"
    labels = {
      "mesh"                                   = local.mesh_name
      "appmesh.k8s.aws/sidecarInjectorWebhook" = "enabled"
    }
  }
  depends_on = [
  null_resource.delete_mesh]
}

resource "null_resource" "delete_mesh" {
  triggers = {
    mesh_name  = local.mesh_name
    kubeconfig = var.kubeconfig
  }
  provisioner "local-exec" {
    when    = destroy
    command = "kubectl --kubeconfig=${self.triggers.kubeconfig} delete mesh ${self.triggers.mesh_name} --ignore-not-found=true"
  }
  depends_on = [
  helm_release.eks]
}

resource "null_resource" "appmesh_readiness_check" {
  provisioner "local-exec" {
    when    = create
    command = "kubectl --kubeconfig=${var.kubeconfig} rollout status deployment ${helm_release.eks.name}-appmesh-controller -n${kubernetes_namespace.appmesh_ns.metadata[0].name}"
  }
}

data "template_file" "traffic_deployment_file" {
  template = file("./appmesh/appmesh_traffic_sample.tpl")
  vars = {
    APP_NAMESPACE   = kubernetes_namespace.traffic_ns.metadata[0].name
    MESH_NAME       = local.mesh_name
    FRONT_APP_IMAGE = "${var.sample_app_image_repo}:feapp-latest"
    COLOR_APP_IMAGE = "${var.sample_app_image_repo}:colorapp-latest"
  }

  depends_on = [
  null_resource.appmesh_readiness_check]
}

resource "local_file" "appmesh_deployment" {
  filename = "appmesh_deployment_${var.testing_id}.yaml"
  content  = data.template_file.traffic_deployment_file.rendered
  depends_on = [
  helm_release.eks]
}

#resource "null_resource" "traffic_deployment" {
#  triggers = {
#    config_contents = md5(local_file.appmesh_deployment.content)
#  }
#  provisioner "local-exec" {
#    command = "kubectl --kubeconfig=${var.kubeconfig} apply -f ${local_file.appmesh_deployment.filename}"
#  }
#}