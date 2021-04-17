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
  default = ""
}

variable "testcase" {
  type    = string
  default = "../testcases/container_insight"
}

output "metric_dimension_namespace" {
  value = kubernetes_namespace.haproxy_ns.metadata[0].name
}

resource "kubernetes_namespace" "haproxy_ns" {
  metadata {
    name = "haproxy-${var.testing_id}"
  }
}

resource "helm_release" "haproxy" {
  name      = "haproxy-${var.testing_id}"
  namespace = kubernetes_namespace.haproxy_ns.metadata[0].name

  repository = "https://haproxy-ingress.github.io/charts"
  chart      = "haproxy-ingress"
  version    = "0.11.4"

  set {
    name  = "defaultBackend.enabled"
    value = "true"
  }
  set {
    name  = "controller.stats.enabled"
    value = "true"
  }
  set {
    name  = "controller.metrics.enabled"
    value = "true"
  }
  set {
    name  = "controller.metrics.service.annotations.prometheus\\.io/port"
    type  = "string"
    value = "9101"
  }
  set {
    name  = "controller.metrics.service.annotations.prometheus\\.io/scrape"
    type  = "string"
    value = "true"
  }
}