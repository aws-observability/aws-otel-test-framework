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
# Push mode deployment for auto_instrumentation
##########################################

# deploy sample app
resource "kubernetes_deployment" "push_mode_sample_app_deployment_auto_instrumentation" {
  count = var.sample_app.mode == "push" && var.auto_instrumentation ? 1 : 0

  metadata {
    name      = "sample-app"
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
          "instrumentation.opentelemetry.io/inject-java" = var.auto_instrumentation
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
            name  = "AWS_REGION"
            value = var.region
          }

          env {
            name  = "INSTANCE_ID"
            value = var.testing_id
          }

          env {
            name  = "LISTEN_ADDRESS"
            value = "${var.sample_app.listen_address_ip}:${var.sample_app.listen_address_port}"
          }

          resources {
            limits = {
              cpu    = "0.2"
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

