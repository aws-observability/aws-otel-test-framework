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

variable "sample_app_image_repo" {
  type    = string
  default = null
}

variable "testcase" {
  type    = string
  default = "../testcases/container_insight"
}

output "metric_dimension_namespace" {
  value = kubernetes_namespace.jmx_ns.metadata[0].name
}

locals {
  traffic_generator_image = "${var.sample_app_image_repo}:traffic-generator"
  jmx_sample_app_image    = "${var.sample_app_image_repo}:tomcatapp"
}

resource "kubernetes_namespace" "jmx_ns" {
  metadata {
    name = "jmx-${var.testing_id}"
  }
}

resource "kubernetes_deployment" "jmx_metric_deployment" {
  metadata {
    name      = "jmx-tomcat"
    namespace = kubernetes_namespace.jmx_ns.metadata[0].name
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "jmx-tomcat"
      }
    }
    template {
      metadata {
        labels = {
          app = "jmx-tomcat"
        }
      }
      spec {
        container {
          name              = "tomcat"
          image             = local.jmx_sample_app_image
          image_pull_policy = "Always"
          resources {
            requests = {
              cpu    = "100m"
              memory = "180Mi"
            }
            limits = {
              cpu    = "200m"
              memory = "300Mi"
            }
          }
          port {
            container_port = 8080
          }
          port {
            container_port = 9404
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "jmx_metric_service" {
  metadata {
    name      = "jmx-tomcat"
    namespace = kubernetes_namespace.jmx_ns.metadata[0].name
  }
  spec {
    selector = {
      app = "jmx-tomcat"
    }
    port {
      port        = 80
      target_port = 8080
      protocol    = "TCP"
    }
  }
}

resource "kubernetes_deployment" "jmx_traffic_deployment" {
  metadata {
    name      = "tomcat-traffic-generator"
    namespace = kubernetes_namespace.jmx_ns.metadata[0].name
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "tomcat-traffic-generator"
      }
    }
    template {
      metadata {
        labels = {
          app = "tomcat-traffic-generator"
        }
      }
      spec {
        container {
          name              = "tomcat-traffic-generator"
          image             = local.traffic_generator_image
          image_pull_policy = "Always"
          command = [
          "/bin/bash"]
          args = [
            "-c",
          "while :; do curl http://jmx-tomcat.${kubernetes_namespace.jmx_ns.metadata[0].name}.svc.cluster.local/tomcatExample/index.jsp; sleep 1s; done"]
        }
      }
    }
  }
}


resource "kubernetes_deployment" "jmx_bad_traffic_deployment" {
  metadata {
    name      = "tomcat-bad-traffic-generator"
    namespace = kubernetes_namespace.jmx_ns.metadata[0].name
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "tomcat-bad-traffic-generator"
      }
    }
    template {
      metadata {
        labels = {
          app = "tomcat-bad-traffic-generator"
        }
      }
      spec {
        container {
          name              = "tomcat-traffic-generator"
          image             = local.traffic_generator_image
          image_pull_policy = "Always"
          command = [
          "/bin/bash"]
          args = [
            "-c",
          "while :; do curl http://jmx-tomcat.${kubernetes_namespace.jmx_ns.metadata[0].name}.svc.cluster.local/error.jsp; sleep .$[($RANDOM%10)]s; done"]
        }
      }
    }
  }
}
