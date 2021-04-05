##########################################
# Pull mode deployments
##########################################

# deploy aoc and mocked server
resource "kubernetes_deployment" "pull_mode_aoc_deployment" {
  count = var.sample_app_mode == "pull" ? 1 : 0

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

        volume {
          name = kubernetes_service_account.aoc-role.default_secret_name
          secret {
            secret_name = kubernetes_service_account.aoc-role.default_secret_name
          }
        }

        container {
          name  = "mocked-server"
          image = local.mocked_server_image

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
            mount_path = "/var/run/secrets/kubernetes.io/serviceaccount"
            name       = kubernetes_service_account.aoc-role.default_secret_name
            read_only  = true
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

# deploy sample app
resource "kubernetes_deployment" "pull_mode_sample_app_deployment" {
  count = var.sample_app_mode == "pull" ? 1 : 0

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
            name  = "AWS_REGION"
            value = var.region
          }

          env {
            name  = "INSTANCE_ID"
            value = module.common.testing_id
          }

          env {
            name  = "LISTEN_ADDRESS"
            value = "${module.common.sample_app_listen_address_ip}:${module.common.sample_app_listen_address_port}"
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

# create service upon the sample app
resource "kubernetes_service" "pull_mode_sample_app_service" {
  count = var.sample_app_mode == "pull" ? 1 : 0

  metadata {
    name      = "sample-app"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }
  spec {
    selector = {
      app = kubernetes_deployment.pull_mode_sample_app_deployment[0].metadata[0].labels.app
    }

    type = "LoadBalancer"

    port {
      port        = module.common.sample_app_lb_port
      target_port = module.common.sample_app_listen_address_port
    }
  }
}