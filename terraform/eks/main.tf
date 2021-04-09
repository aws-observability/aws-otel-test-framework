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
  required_providers {
    kubernetes = {
      version = "~> 1.13"
    }
  }
}

module "common" {
  source = "../common"

  aoc_image_repo = var.aoc_image_repo
  aoc_version    = var.aoc_version
}

module "basic_components" {
  source = "../basic_components"

  region = var.region

  testcase = var.testcase

  testing_id = module.common.testing_id

  mocked_endpoint = replace(var.mock_endpoint, "mocked-server", "localhost")

  sample_app = var.sample_app

  mocked_server = var.mocked_server

  cortex_instance_endpoint = var.cortex_instance_endpoint

  sample_app_listen_address_host = var.sample_app_mode == "pull" ? kubernetes_service.pull_mode_sample_app_service[0].load_balancer_ingress.0.hostname : ""

  sample_app_listen_address_port = module.common.sample_app_lb_port
}

locals {
  eks_pod_config_path = fileexists("${var.testcase}/eks_pod_config.tpl") ? "${var.testcase}/eks_pod_config.tpl" : module.common.default_eks_pod_config_path
  sample_app_image    = var.sample_app_image != "" ? var.sample_app_image : module.basic_components.sample_app_image
  mocked_server_image = var.mocked_server_image != "" ? var.mocked_server_image : module.basic_components.mocked_server_image
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

# set up kubectl
provider "kubernetes" {
  host                   = data.aws_eks_cluster.testing_cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.testing_cluster.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.testing_cluster.token
  load_config_file       = false
}

# create a unique namespace for each run
resource "kubernetes_namespace" "aoc_ns" {
  metadata {
    name = "aoc-ns-${module.common.testing_id}"
  }
}
resource "kubernetes_config_map" "aoc_config_map" {
  metadata {
    name      = "otel-config"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }

  data = {
    "aoc-config.yml" = module.basic_components.otconfig_content
  }
}

# load eks pod config
data "template_file" "eksconfig" {
  template = file(local.eks_pod_config_path)

  vars = {
    data_emitter_image = local.sample_app_image
    testing_id         = module.common.testing_id
  }
}

# load the faked cert for mocked server
resource "kubernetes_config_map" "mocked_server_cert" {
  metadata {
    name      = "mocked-server-cert"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }

  data = {
    "ca-bundle.crt" = module.basic_components.mocked_server_cert_content
  }
}

locals {
  eks_pod_config = yamldecode(data.template_file.eksconfig.rendered)["sample_app"]
}

resource "kubernetes_service_account" "aoc-role" {
  metadata {
    name      = "aoc-role-${module.common.testing_id}"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }

  automount_service_account_token = true
}

resource "kubernetes_cluster_role_binding" "aoc-role-binding" {
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
    name      = "aoc-role-${module.common.testing_id}"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }
}

##########################################
# Push mode deployments
##########################################

# deploy aoc and mocked server
resource "kubernetes_deployment" "aoc_deployment" {
  count = var.sample_app_mode == "push" ? 1 : 0

  metadata {
    name      = "aoc"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
    labels = {
      app = "aoc"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "aoc"
      }
    }

    template {
      metadata {
        labels = {
          app = "aoc"
        }
      }


      spec {
        service_account_name = "aoc-role-${module.common.testing_id}"

        volume {
          name = "otel-config"
          config_map {
            name = kubernetes_config_map.aoc_config_map.metadata[0].name
          }
        }

        volume {
          name = "mocked-server-cert"
          config_map {
            name = kubernetes_config_map.mocked_server_cert.metadata[0].name
          }
        }

        container {
          name              = "mocked-server"
          image             = local.mocked_server_image
          image_pull_policy = "Always"

          readiness_probe {
            http_get {
              path = "/"
              port = 8080
            }
            initial_delay_seconds = 10
            period_seconds        = 5
          }
        }

        # aoc
        container {
          name              = "aoc"
          image             = module.common.aoc_image
          image_pull_policy = "Always"
          args = [
          "--config=/aoc/aoc-config.yml"]

          resources {
            requests {
              cpu    = "0.2"
              memory = "256Mi"
            }
          }

          volume_mount {
            mount_path = "/aoc"
            name       = "otel-config"
          }

          volume_mount {
            mount_path = "/etc/pki/tls/certs"
            name       = "mocked-server-cert"
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
  count = var.sample_app_mode == "push" ? 1 : 0

  metadata {
    name      = "aoc-grpc"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }
  spec {
    selector = {
      app = kubernetes_deployment.aoc_deployment[0].metadata[0].labels.app
    }

    port {
      port        = module.common.grpc_port
      target_port = module.common.grpc_port
      protocol    = "TCP"
    }
  }
}

# create service upon AOC (UDP port)
resource "kubernetes_service" "aoc_udp_service" {
  count = var.sample_app_mode == "push" ? 1 : 0

  metadata {
    name      = "aoc-udp"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }
  spec {
    selector = {
      app = kubernetes_deployment.aoc_deployment[0].metadata[0].labels.app
    }

    port {
      port        = module.common.udp_port
      target_port = module.common.udp_port
      protocol    = "UDP"
    }
  }
}

# create service upon AOC (TCP port)
resource "kubernetes_service" "aoc_tcp_service" {
  count = var.sample_app_mode == "push" ? 1 : 0

  metadata {
    name      = "aoc-tcp"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }
  spec {
    selector = {
      app = kubernetes_deployment.aoc_deployment[0].metadata[0].labels.app
    }

    port {
      port        = module.common.http_port
      target_port = module.common.http_port
    }
  }
}

# deploy sample app
resource "kubernetes_deployment" "sample_app_deployment" {
  count = var.sample_app_mode == "push" ? 1 : 0

  metadata {
    name      = "sample-app"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
    labels = {
      app = "sample-app"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "sample-app"
      }
    }

    template {
      metadata {
        labels = {
          app = "sample-app"
        }
      }

      spec {
        # sample app
        container {
          name              = "sample-app"
          image             = local.eks_pod_config["image"]
          image_pull_policy = "Always"
          command           = length(local.eks_pod_config["command"]) != 0 ? local.eks_pod_config["command"] : null
          args              = length(local.eks_pod_config["args"]) != 0 ? local.eks_pod_config["args"] : null


          env {
            name  = "OTEL_EXPORTER_OTLP_ENDPOINT"
            value = "http://${kubernetes_service.aoc_grpc_service[0].metadata[0].name}:${module.common.grpc_port}"
          }

          env {
            name  = "COLLECTOR_UDP_ADDRESS"
            value = "${kubernetes_service.aoc_udp_service[0].metadata[0].name}:${module.common.udp_port}"
          }

          env {
            name  = "AWS_XRAY_DAEMON_ADDRESS"
            value = "${kubernetes_service.aoc_udp_service[0].metadata[0].name}:${module.common.udp_port}"
          }

          env {
            name  = "AWS_REGION"
            value = var.region
          }

          env {
            name  = "INSTANCE_ID"
            value = module.common.testing_id
          }

          env {
            name  = "OTEL_RESOURCE_ATTRIBUTES"
            value = "service.namespace=${module.common.otel_service_namespace},service.name=${module.common.otel_service_name}"
          }

          env {
            name  = "LISTEN_ADDRESS"
            value = "${module.common.sample_app_listen_address_ip}:${module.common.sample_app_listen_address_port}"
          }

          env {
            name  = "JAEGER_RECEIVER_ENDPOINT"
            value = "${kubernetes_service.aoc_tcp_service[0].metadata[0].name}:${module.common.http_port}"
          }

          env {
            name  = "ZIPKIN_RECEIVER_ENDPOINT"
            value = "${kubernetes_service.aoc_tcp_service[0].metadata[0].name}:${module.common.http_port}"
          }

          resources {
            requests {
              cpu    = "0.2"
              memory = "256Mi"
            }

          }

          readiness_probe {
            http_get {
              path = "/"
              port = module.common.sample_app_listen_address_port
            }
            initial_delay_seconds = 10
            period_seconds        = 5
          }
        }
      }
    }
  }
}

##########################################
# Common components for both push and pull modes
##########################################

# create service upon the mocked server
resource "kubernetes_service" "mocked_server_service" {
  metadata {
    name      = "mocked-server"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }
  spec {
    selector = {
      app = var.sample_app_mode == "push" ? kubernetes_deployment.aoc_deployment[0].metadata[0].labels.app : kubernetes_deployment.pull_mode_aoc_deployment[0].metadata[0].labels.app
    }

    type = "LoadBalancer"

    port {
      port        = 80
      target_port = 8080
    }
  }
}

# create service upon the sample app
resource "kubernetes_service" "sample_app_service" {
  count = var.sample_app_mode == "push" ? 1 : 0

  metadata {
    name      = "sample-app"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }
  spec {
    selector = {
      app = kubernetes_deployment.sample_app_deployment[0].metadata[0].labels.app
    }

    type = "LoadBalancer"

    port {
      port        = module.common.sample_app_lb_port
      target_port = module.common.sample_app_listen_address_port
    }
  }
}

##########################################
# Validation
##########################################
module "validator" {
  source = "../validation"

  validation_config            = var.validation_config
  region                       = var.region
  testing_id                   = module.common.testing_id
  metric_namespace             = "${module.common.otel_service_namespace}/${module.common.otel_service_name}"
  sample_app_endpoint          = "http://${var.sample_app_mode == "push" ? kubernetes_service.sample_app_service[0].load_balancer_ingress.0.hostname : kubernetes_service.pull_mode_sample_app_service[0].load_balancer_ingress.0.hostname}:${module.common.sample_app_lb_port}"
  mocked_server_validating_url = "http://${kubernetes_service.mocked_server_service.load_balancer_ingress.0.hostname}/check-data"

  cortex_instance_endpoint = var.cortex_instance_endpoint

  aws_access_key_id     = var.aws_access_key_id
  aws_secret_access_key = var.aws_secret_access_key

  depends_on = [
  kubernetes_service.mocked_server_service]
}
