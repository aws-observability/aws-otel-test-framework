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
  count  = var.aoc_base_scenario == "aoc_standalone_daemonset" ? 1 : 0

  region                         = var.region
  testcase                       = var.testcase
  testing_id                     = module.common.testing_id
  debug                          = var.debug
}

locals {
  aoc_label_selector  = "aoc"
}

resource "kubernetes_config_map" "aoc_config_map" {
  count = var.aoc_base_scenario == "aoc_standalone_daemonset" && replace(var.testcase, "_adot_operator", "") == var.testcase && var.deployment_type != "fargate"? 1 : 0

  metadata {
    name      = "otel-config"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }

  data = {
    "aoc-config.yml" = module.basic_components.0.otconfig_content
  }
}

# deploy aoc and mocked server
resource "kubernetes_daemonset" "aoc_deployment" {
  count = var.aoc_base_scenario == "aoc_standalone_daemonset" && replace(var.testcase, "_adot_operator", "") == var.testcase && var.deployment_type != "fargate" && var.aoc_deploy_mode == "daemonset" ? 1 : 0

  metadata {
    name      = "aoc"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
    labels = {
      app = "aoc"
    }
  }

  spec {
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

        # aoc
        container {
          name              = "aoc"
          image             = module.common.aoc_image
          image_pull_policy = "Always"
          args = [
            "--config=/aoc/aoc-config.yml"]

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
        }
      }
    }
  }
}
