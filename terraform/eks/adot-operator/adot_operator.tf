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

variable "testing_id" {
  type    = string
  default = ""
}

variable "kubeconfig" {
  type    = string
  default = "kubeconfig"
}

variable "operator_repository" {
}

variable "operator_tag" {
}

variable "aoc_image_repo" {
}

variable "aoc_version" {
}

resource "kubernetes_namespace" "adot_operator_ns" {
  metadata {
    name = "adot-operator-${var.testing_id}-ns"
  }
}

# The operator discovers the Kubernetes API server at startup by listing the
# EndpointSlices of the kubernetes service in the default namespace. The chart's
# bundled manager role does not grant this, so add it out of band, scoped to this
# test's operator service account.
resource "kubernetes_cluster_role" "adot_operator_endpointslices" {
  metadata {
    name = "adot-operator-${var.testing_id}-endpointslices"
  }

  rule {
    api_groups = ["discovery.k8s.io"]
    resources  = ["endpointslices"]
    verbs      = ["get", "list"]
  }
}

resource "kubernetes_cluster_role_binding" "adot_operator_endpointslices" {
  metadata {
    name = "adot-operator-${var.testing_id}-endpointslices"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = kubernetes_cluster_role.adot_operator_endpointslices.metadata[0].name
  }

  subject {
    kind      = "ServiceAccount"
    name      = "opentelemetry-operator"
    namespace = kubernetes_namespace.adot_operator_ns.metadata[0].name
  }
}

resource "helm_release" "adot-operator" {
  name = "adot-operator-${var.testing_id}"

  repository = "https://open-telemetry.github.io/opentelemetry-helm-charts"
  chart      = "opentelemetry-operator"
  version    = "0.100.0"
  namespace  = "adot-operator-${var.testing_id}-ns"
  wait       = true
  timeout    = 600

  values = [
    file("./adot-operator/adot-operator-values.yaml")
  ]

  set {
    name  = "manager.image.repository"
    value = var.operator_repository
  }

  set {
    name  = "manager.image.tag"
    value = var.operator_tag
  }

  set {
    name  = "manager.collectorImage.tag"
    value = var.aoc_version
  }

  set {
    name  = "manager.collectorImage.repository"
    value = var.aoc_image_repo
  }

  depends_on = [
    kubernetes_namespace.adot_operator_ns,
    kubernetes_cluster_role_binding.adot_operator_endpointslices,
  ]
}
