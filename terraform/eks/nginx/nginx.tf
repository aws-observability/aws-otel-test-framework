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

variable "kubeconfig" {
  type    = string
  default = "kubeconfig"
}

output "metric_dimension_namespace" {
  value = kubernetes_namespace.nginx_ns.metadata[0].name
}

resource "kubernetes_namespace" "nginx_ns" {
  metadata {
    name = "nginx-${var.testing_id}"
  }
}

resource "kubernetes_namespace" "traffic_ns" {
  metadata {
    name = "nginx-sample-traffic-${var.testing_id}"
  }
}

resource "helm_release" "nginx_ingress" {
  name      = "nginx-${var.testing_id}"
  namespace = kubernetes_namespace.nginx_ns.metadata[0].name

  repository = "https://kubernetes.github.io/ingress-nginx"
  chart      = "ingress-nginx"
  version    = "4.4.0"

  set {
    name  = "controller.metrics.enabled"
    value = "true"
  }
  set {
    name  = "controller.metrics.service.annotations.prometheus\\.io/port"
    type  = "string"
    value = "10254"
  }
  set {
    name  = "controller.metrics.service.annotations.prometheus\\.io/scrape"
    type  = "string"
    value = "true"
  }

  provisioner "local-exec" {
    command = "/bin/bash ./nginx/get-service-external-ip.sh"
    environment = {
      KUBECONFIG   = var.kubeconfig
      NAMESPACE    = self.namespace
      SERVICE_NAME = "${self.name}-ingress-nginx-controller"
    }
  }
}

resource "null_resource" "apply_traffic_deployment" {
  depends_on = [helm_release.nginx_ingress]

  provisioner "local-exec" {
    command = "/bin/bash ./nginx/apply-traffic.sh"

    environment = {
      KUBECONFIG = var.kubeconfig
      NS         = kubernetes_namespace.nginx_ns.metadata[0].name
      SVC        = "${helm_release.nginx_ingress.name}-ingress-nginx-controller"
      NAMESPACE  = kubernetes_namespace.traffic_ns.metadata[0].name
    }
  }
}