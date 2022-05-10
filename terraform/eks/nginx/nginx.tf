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
  version    = "3.41.0"

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

data "kubernetes_service" "nginx_ingress_sample" {
  metadata {
    name      = "${helm_release.nginx_ingress.name}-ingress-nginx-controller"
    namespace = kubernetes_namespace.nginx_ns.metadata[0].name
  }
  depends_on = [helm_release.nginx_ingress]
}

data "template_file" "traffic_deployment_file" {
  template = file("./nginx/nginx_traffic_sample.tpl")
  vars = {
    NAMESPACE   = kubernetes_namespace.traffic_ns.metadata[0].name
    EXTERNAL_IP = data.kubernetes_service.nginx_ingress_sample.load_balancer_ingress.0.hostname
  }
}

resource "local_file" "traffic_deployment" {
  filename = "nginx_traffic_sample_${var.testing_id}.yaml"
  content  = data.template_file.traffic_deployment_file.rendered
}

resource "null_resource" "apply_traffic_deployment" {
  triggers = {
    config_contents = md5(local_file.traffic_deployment.content)
  }
  provisioner "local-exec" {
    command = "kubectl --kubeconfig=${var.kubeconfig} apply -f ${local_file.traffic_deployment.filename}"
  }
}