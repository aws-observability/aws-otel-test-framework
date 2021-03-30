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
  type = string
  default = "../testcases/container_insight"
}

output "metric_dimension_namespace" {
  value = kubernetes_namespace.memcached_ns.metadata[0].name
}

resource "kubernetes_namespace" "memcached_ns" {
  metadata {
    name = "memcached-${var.testing_id}"
  }
}

resource "helm_release" "bitnami" {
  name = "memcached"
  namespace = kubernetes_namespace.memcached_ns.metadata[0].name

  repository = "https://charts.bitnami.com/bitnami"
  chart = "memcached"
  version = "5.8.1"

  set {
    name = "metrics.enabled"
    value = "true"
  }
  set {
    name = "serviceAnnotations.prometheus\\.io/port"
    type = "string"
    value = "9150"
  }
  set {
    name = "serviceAnnotations.prometheus\\.io/scrape"
    type = "string"
    value = "true"
  }
}