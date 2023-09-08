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

module "aoc_msk_cluster" {
  source              = "../data_aoc_msk"
  cluster_name_prefix = "EKSMSKCluster"
  cluster_version     = var.kafka_version
  testcase            = var.testcase
  dedup_topic         = "eks${var.eks_cluster_name}"
}

locals {
  is_otlp_base_scenario = var.aoc_base_scenario == "otlp"
  is_operator_testcase  = strcontains(var.testcase, "_adot_operator")
}

module "basic_components" {
  source = "../basic_components"
  count  = local.is_otlp_base_scenario ? 1 : 0

  region                         = var.region
  testcase                       = var.testcase
  testing_id                     = module.common.testing_id
  mocked_endpoint                = replace(var.mock_endpoint, "mocked-server", "localhost")
  sample_app                     = var.sample_app
  mocked_server                  = var.mocked_server
  cortex_instance_endpoint       = var.cortex_instance_endpoint
  sample_app_listen_address_host = var.sample_app_mode == "pull" ? kubernetes_service.sample_app_service[0].status[0].load_balancer[0].ingress[0].hostname : ""
  sample_app_listen_address_port = module.common.sample_app_lb_port

  extra_data = { msk = module.aoc_msk_cluster.cluster_data }

  debug = var.debug
}

module "remote_configuration" {
  count  = var.configuration_source != "file" && local.is_otlp_base_scenario ? 1 : 0
  source = "../remote_configuration"

  content    = module.basic_components[0].otconfig_content
  scheme     = var.configuration_source
  testing_id = module.common.testing_id
}

# create an IAM role here so that we can reference the clusters OIDC Provider.
# This will be used for the push mode sample app since it needs to make a call to s3.listBuckets()
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

# service acount name will be passed to the otlp module for use in the push mode sample app
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

module "aoc_otlp" {
  source = "./otlp"
  count  = local.is_otlp_base_scenario ? 1 : 0

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
  aoc_namespace = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
  aoc_service = {
    name      = module.common.otel_service_name
    grpc_port = module.common.grpc_port
    udp_port  = module.common.udp_port
    http_port = module.common.http_port
  }
  sample_app_service_account_name = kubernetes_service_account.sample-app-sa.metadata.0.name
  is_adot_operator                = local.is_operator_testcase
  is_inject_auto_instrumentation  = var.is_inject_auto_instrumentation

  depends_on = [module.iam_assumable_role_sample_app, kubectl_manifest.java_auto_instrumentation_deployment]
}

locals {
  aoc_label_selector  = "aoc"
  mocked_server_image = length(module.basic_components) > 0 ? (var.mocked_server_image != "" ? var.mocked_server_image : module.basic_components.0.mocked_server_image) : ""
}

resource "kubernetes_config_map" "aoc_config_map" {
  count = local.is_otlp_base_scenario && !local.is_operator_testcase ? 1 : 0

  metadata {
    name      = "otel-config"
    namespace = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
  }

  data = {
    "aoc-config.yml" = module.basic_components.0.otconfig_content
  }
  depends_on = [kubernetes_service_account.sample-app-sa]
}

# load the faked cert for mocked server
resource "kubernetes_config_map" "mocked_server_cert" {
  count = local.is_otlp_base_scenario && !local.is_operator_testcase ? 1 : 0

  metadata {
    name      = "mocked-server-cert"
    namespace = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
  }

  data = {
    "ca-bundle.crt" = module.basic_components.0.mocked_server_cert_content
  }
}

locals {
  configuration_uri = var.configuration_source == "file" ? "/aoc/aoc-config.yml" : module.remote_configuration[0].configuration_uri
  collector_args = length(var.feature_gate) != 0 ? [
    "--config", local.configuration_uri, "--feature-gates", var.feature_gate
    ] : [
    "--config", local.configuration_uri
  ]
}

# deploy aoc and mocked server
resource "kubernetes_deployment" "aoc_deployment" {
  count = local.is_otlp_base_scenario && !local.is_operator_testcase ? 1 : 0

  metadata {
    name      = "aoc"
    namespace = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
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
          args              = local.collector_args

          resources {
            limits = {
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

# create service upon the mocked server
resource "kubernetes_service" "mocked_server_service" {
  count = local.is_otlp_base_scenario && !local.is_operator_testcase ? 1 : 0

  metadata {
    name      = "mocked-server"
    namespace = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
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
}

data "template_file" "adot_collector_config_file" {
  count = local.is_otlp_base_scenario && local.is_operator_testcase ? 1 : 0

  template = file("./adot-operator/adot_collector_deployment.tpl")

  vars = {
    AOC_NAMESPACE      = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
    AOC_IMAGE          = module.common.aoc_image
    AOC_DEPLOY_MODE    = var.aoc_deploy_mode
    AOC_SERVICEACCOUNT = "aoc-role-${module.common.testing_id}"
    AOC_CONFIG         = module.basic_components.0.otconfig_content
  }

  depends_on = [module.adot_operator]
}

resource "local_file" "adot_collector_deployment" {
  count = local.is_otlp_base_scenario && local.is_operator_testcase ? 1 : 0

  filename = "adot_collector.yaml"
  content  = data.template_file.adot_collector_config_file.0.rendered

  depends_on = [module.adot_operator]
}

resource "null_resource" "aoc_deployment_adot_operator" {
  count = local.is_otlp_base_scenario && local.is_operator_testcase ? 1 : 0

  provisioner "local-exec" {
    command = "kubectl apply --kubeconfig=${local_file.kubeconfig.filename} -f ${local_file.adot_collector_deployment.0.filename}"
  }

  depends_on = [module.adot_operator]
}

resource "kubectl_manifest" "java_auto_instrumentation_deployment" {
  count = local.is_otlp_base_scenario && local.is_operator_testcase && var.is_inject_auto_instrumentation ? 1 : 0

  yaml_body  = <<-EOF
    apiVersion: opentelemetry.io/v1alpha1
    kind: Instrumentation
    metadata:
      name: my-instrumentation
      namespace: ${var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name}
    spec:
      sampler:
        type: parentbased_traceidratio
        argument: "1.0"
      java:
        image: ${var.java_auto_instrumentation_repository}:${var.java_auto_instrumentation_tag}
    EOF
  depends_on = [module.adot_operator]
}

resource "kubernetes_service" "sample_app_service" {
  count = local.is_otlp_base_scenario ? 1 : 0

  metadata {
    name      = "sample-app"
    namespace = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
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
}

resource "kubernetes_ingress" "app" {
  count                  = var.deployment_type == "fargate" && local.is_otlp_base_scenario ? 1 : 0
  wait_for_load_balancer = true
  metadata {
    name      = "sample-app-ingress"
    namespace = kubernetes_namespace.aoc_fargate_ns.metadata[0].name
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

