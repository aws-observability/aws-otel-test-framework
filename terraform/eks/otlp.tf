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

module "basic_components" {
  source = "../basic_components"
  count  = var.aoc_base_scenario == "oltp" ? 1 : 0

  region                         = var.region
  testcase                       = var.testcase
  testing_id                     = module.common.testing_id
  mocked_endpoint                = replace(var.mock_endpoint, "mocked-server", "localhost")
  sample_app                     = var.sample_app
  mocked_server                  = var.mocked_server
  cortex_instance_endpoint       = var.cortex_instance_endpoint
  sample_app_listen_address_host = var.sample_app_mode == "pull" ? kubernetes_service.sample_app_service.0.load_balancer_ingress.0.hostname : ""
  sample_app_listen_address_port = module.common.sample_app_lb_port
}

# crete an IAM role here so that we can reference the clusters OIDC Provider
# this will be used for the push mode sample app since it needs to make a call to s3.listBuckets()
module "iam_assumable_role_sample_app" {
  create_role = true

  role_name = "push-mode-sample-app--${module.common.testing_id}"

  provider_url = trimprefix(data.aws_eks_cluster.testing_cluster.identity[0].oidc[0].issuer, "https://")

  role_policy_arns = [
    "arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess",
  ]
  source  = "terraform-aws-modules/iam/aws//modules/iam-assumable-role-with-oidc"
  version = "4.7.0"
}

# servic acount name will be passed to the otlp module for use in the push mode sample app
resource "kubernetes_service_account" "sample-app-sa" {
  metadata {
    name      = "sample-app-sa-${module.common.testing_id}"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
    annotations = {
      "eks.amazonaws.com/role-arn" : module.iam_assumable_role_sample_app.iam_role_arn
    }
  }

  automount_service_account_token = true
  depends_on = [
    module.iam_assumable_role_sample_app
  ]
}

module "aoc_oltp" {
  source = "./otlp"
  count  = var.aoc_base_scenario == "oltp" ? 1 : 0

  region              = var.region
  testing_id          = module.common.testing_id
  eks_pod_config_path = fileexists("${var.testcase}/eks_pod_config.tpl") ? "${var.testcase}/eks_pod_config.tpl" : module.common.default_eks_pod_config_path
  sample_app = {
    image               = var.sample_app_image != "" ? var.sample_app_image : module.basic_components.0.sample_app_image
    name                = var.sample_app
    mode                = var.sample_app_mode
    metric_namespace    = module.common.otel_service_namespace
    listen_address_ip   = module.common.sample_app_listen_address_ip
    listen_address_port = module.common.sample_app_listen_address_port
  }
  aoc_namespace = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile[count.index].selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
  aoc_service = {
    name      = module.common.otel_service_name
    grpc_port = module.common.grpc_port
    udp_port  = module.common.udp_port
    http_port = module.common.http_port
  }
  sample_app_service_account_name = kubernetes_service_account.sample-app-sa.metadata.0.name
  is_adot_operator                = replace(var.testcase, "_adot_operator", "") != var.testcase
  depends_on                      = [aws_eks_fargate_profile.test_profile, module.iam_assumable_role_sample_app]
}

locals {
  aoc_label_selector  = "aoc"
  mocked_server_image = length(module.basic_components) > 0 ? (var.mocked_server_image != "" ? var.mocked_server_image : module.basic_components.0.mocked_server_image) : ""
}

resource "kubernetes_config_map" "aoc_config_map" {
  count = var.aoc_base_scenario == "oltp" && replace(var.testcase, "_adot_operator", "") == var.testcase ? 1 : 0

  metadata {
    name      = "otel-config"
    namespace = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile[count.index].selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
  }

  data = {
    "aoc-config.yml" = module.basic_components.0.otconfig_content
  }
  depends_on = [aws_eks_fargate_profile.test_profile, kubernetes_service_account.sample-app-sa]
}

# load the faked cert for mocked server
resource "kubernetes_config_map" "mocked_server_cert" {
  count = var.aoc_base_scenario == "oltp" && replace(var.testcase, "_adot_operator", "") == var.testcase ? 1 : 0

  metadata {
    name      = "mocked-server-cert"
    namespace = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile[count.index].selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
  }

  data = {
    "ca-bundle.crt" = module.basic_components.0.mocked_server_cert_content
  }
  depends_on = [aws_eks_fargate_profile.test_profile]
}

# deploy aoc and mocked server
resource "kubernetes_deployment" "aoc_deployment" {
  count = var.aoc_base_scenario == "oltp" && replace(var.testcase, "_adot_operator", "") == var.testcase ? 1 : 0

  metadata {
    name      = "aoc"
    namespace = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile[count.index].selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
    labels = {
      app = "aoc"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = local.aoc_label_selector
      }
    }

    template {
      metadata {
        labels = {
          app = local.aoc_label_selector
        }
      }

      spec {
        service_account_name            = "aoc-role-${module.common.testing_id}"
        automount_service_account_token = true

        volume {
          name = "otel-config"
          config_map {
            name = kubernetes_config_map.aoc_config_map.0.metadata[0].name
          }
        }

        volume {
          name = "mocked-server-cert"
          config_map {
            name = kubernetes_config_map.mocked_server_cert.0.metadata[0].name
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
            limits {
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
  depends_on = [aws_eks_fargate_profile.test_profile]
}

# create service upon the mocked server
resource "kubernetes_service" "mocked_server_service" {
  count = var.aoc_base_scenario == "oltp" && replace(var.testcase, "_adot_operator", "") == var.testcase ? 1 : 0

  metadata {
    name      = "mocked-server"
    namespace = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile[count.index].selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
  }
  spec {
    selector = {
      app = local.aoc_label_selector
    }
    type = "LoadBalancer"
    port {
      port        = 80
      target_port = 8080
    }
  }
  depends_on = [aws_eks_fargate_profile.test_profile]
}

data "template_file" "adot_collector_config_file" {
  count = var.aoc_base_scenario == "oltp" && replace(var.testcase, "_adot_operator", "") != var.testcase ? 1 : 0

  template = file("./adot-operator/adot_collector_deployment.tpl")

  vars = {
    AOC_NAMESPACE      = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile[count.index].selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
    AOC_IMAGE          = module.common.aoc_image
    AOC_DEPLOY_MODE    = var.aoc_deploy_mode
    AOC_SERVICEACCOUNT = "aoc-role-${module.common.testing_id}"
    AOC_CONFIG         = module.basic_components.0.otconfig_content
  }

  depends_on = [module.adot_operator, aws_eks_fargate_profile.test_profile]
}

resource "local_file" "adot_collector_deployment" {
  count = var.aoc_base_scenario == "oltp" && replace(var.testcase, "_adot_operator", "") != var.testcase ? 1 : 0

  filename = "adot_collector.yaml"
  content  = data.template_file.adot_collector_config_file.0.rendered

  depends_on = [module.adot_operator]
}

resource "null_resource" "aoc_deployment_adot_operator" {
  count = var.aoc_base_scenario == "oltp" && replace(var.testcase, "_adot_operator", "") != var.testcase ? 1 : 0

  provisioner "local-exec" {
    command = "kubectl apply --kubeconfig=${local_file.kubeconfig.filename} -f ${local_file.adot_collector_deployment.0.filename}"
  }

  depends_on = [module.adot_operator]
}

resource "kubernetes_service" "sample_app_service" {
  count = var.aoc_base_scenario == "oltp" ? 1 : 0

  metadata {
    name      = "sample-app"
    namespace = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile[count.index].selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
  }
  spec {
    selector = {
      app = "sample-app"
    }

    type = var.deployment_type == "fargate" ? "NodePort" : "LoadBalancer"

    port {
      port        = module.common.sample_app_lb_port
      target_port = module.common.sample_app_listen_address_port
    }
  }
  depends_on = [aws_eks_fargate_profile.test_profile]
}

resource "kubernetes_ingress" "app" {
  count                  = var.deployment_type == "fargate" && var.aoc_base_scenario == "oltp" ? 1 : 0
  wait_for_load_balancer = true
  metadata {
    name      = "sample-app-ingress"
    namespace = tolist(aws_eks_fargate_profile.test_profile[count.index].selector)[0].namespace
    annotations = {
      "kubernetes.io/ingress.class"           = "alb"
      "alb.ingress.kubernetes.io/scheme"      = "internet-facing"
      "alb.ingress.kubernetes.io/target-type" = "ip"
    }
    labels = {
      "app" = "sample-app"
    }
  }

  spec {
    rule {
      http {
        path {
          path = "/*"
          backend {
            service_name = kubernetes_service.sample_app_service[count.index].metadata[0].name
            service_port = kubernetes_service.sample_app_service[count.index].spec[0].port[0].port
          }
        }
      }
    }
  }

  depends_on = [kubernetes_service.sample_app_service]
}

