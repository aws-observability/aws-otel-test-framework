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

##########################################
# Push mode deployments
##########################################

locals {
  aoc_label_selector        = "aoc"
  sample_app_label_selector = "sample-app"
}

# deploy sample app
resource "kubernetes_deployment" "push_mode_sample_app_deployment" {
  count = var.sample_app.mode == "push" ? 1 : 0

  metadata {
    name      = var.sample_app_deployment_name
    namespace = var.aoc_namespace
    labels = {
      app = "sample-app"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = local.sample_app_label_selector
      }
    }

    template {
      metadata {
        labels = {
          app = local.sample_app_label_selector
        }
        annotations = {
          "instrumentation.opentelemetry.io/inject-java" = var.is_inject_auto_instrumentation
        }
      }

      spec {
        # sample app
        service_account_name = var.sample_app_service_account_name
        container {
          name              = "sample-app"
          image             = local.eks_pod_config["image"]
          image_pull_policy = "Always"
          command           = length(local.eks_pod_config["command"]) != 0 ? local.eks_pod_config["command"] : null
          args              = length(local.eks_pod_config["args"]) != 0 ? local.eks_pod_config["args"] : null
          env {
            name = "K8S_POD_NAME"
            value_from {
              field_ref {
                field_path = "metadata.name"
              }
            }
          }

          env {
            name = "K8S_NAMESPACE"
            value_from {
              field_ref {
                field_path = "metadata.namespace"
              }
            }
          }

          env {
            name  = "OTEL_EXPORTER_OTLP_ENDPOINT"
            value = var.is_adot_operator ? "http://aoc-collector:${var.aoc_service.grpc_port}" : "http://${kubernetes_service.aoc_grpc_service[0].metadata[0].name}:${var.aoc_service.grpc_port}"
          }

          env {
            name  = "COLLECTOR_UDP_ADDRESS"
            value = "${kubernetes_service.aoc_udp_service[0].metadata[0].name}:${var.aoc_service.udp_port}"
          }

          env {
            name  = "AWS_XRAY_DAEMON_ADDRESS"
            value = "${kubernetes_service.aoc_udp_service[0].metadata[0].name}:${var.aoc_service.udp_port}"
          }

          env {
            name  = "AWS_REGION"
            value = var.region
          }

          env {
            name  = "INSTANCE_ID"
            value = var.testing_id
          }

          env {
            name  = "OTEL_RESOURCE_ATTRIBUTES"
            value = "service.namespace=${var.sample_app.metric_namespace},k8s.pod.name=$(K8S_POD_NAME),k8s.namespace.name=$(K8S_NAMESPACE)"
          }

          env {
            name  = "OTEL_SERVICE_NAME"
            value = var.aoc_service.name
          }

          env {
            name  = "LISTEN_ADDRESS"
            value = "${var.sample_app.listen_address_ip}:${var.sample_app.listen_address_port}"
          }

          env {
            name  = "JAEGER_RECEIVER_ENDPOINT"
            value = "${kubernetes_service.aoc_tcp_service[0].metadata[0].name}:${var.aoc_service.http_port}"
          }

          env {
            name  = "ZIPKIN_RECEIVER_ENDPOINT"
            value = "${kubernetes_service.aoc_tcp_service[0].metadata[0].name}:${var.aoc_service.http_port}"
          }

          env {
            name  = "OTEL_METRICS_EXPORTER"
            value = "otlp"
          }

          resources {
            limits = {
              cpu    = "100m"
              memory = "256Mi"
            }
          }

          readiness_probe {
            http_get {
              path = "/"
              port = var.sample_app.listen_address_port
            }
            initial_delay_seconds = 10
            period_seconds        = 5
          }
        }
      }
    }
  }
}


# create service upon AOC (GRPC port)
# NOTE: we have to create a service for each port protocol type because
# Kubernetes does not support mixed protocols: https://github.com/kubernetes/kubernetes/pull/64471.
resource "kubernetes_service" "aoc_grpc_service" {
  count = var.sample_app.mode == "push" ? 1 : 0

  metadata {
    name      = "aoc-grpc"
    namespace = var.aoc_namespace
  }
  spec {
    selector = {
      app = local.aoc_label_selector
    }

    port {
      port        = var.aoc_service.grpc_port
      target_port = var.aoc_service.grpc_port
      protocol    = "TCP"
    }
  }
}

# create service upon AOC (UDP port)
resource "kubernetes_service" "aoc_udp_service" {
  count = var.sample_app.mode == "push" ? 1 : 0

  metadata {
    name      = "aoc-udp"
    namespace = var.aoc_namespace
  }
  spec {
    selector = {
      app = local.aoc_label_selector
    }

    port {
      port        = var.aoc_service.udp_port
      target_port = var.aoc_service.udp_port
      protocol    = "UDP"
    }
  }
}

# create service upon AOC (TCP port)
resource "kubernetes_service" "aoc_tcp_service" {
  count = var.sample_app.mode == "push" ? 1 : 0

  metadata {
    name      = "aoc-tcp"
    namespace = var.aoc_namespace
  }
  spec {
    selector = {
      app = local.aoc_label_selector
    }

    port {
      port        = var.aoc_service.http_port
      target_port = var.aoc_service.http_port
    }
  }
}
