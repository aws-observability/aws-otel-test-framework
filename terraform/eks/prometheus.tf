resource "kubernetes_deployment" "standalone_aoc_deployment" {
  count = var.aoc_base_scenario == "prometheus" ? 1 : 0
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
        service_account_name            = "aoc-role-${module.common.testing_id}"
        automount_service_account_token = true
        # aoc
        container {
          name              = "aoc"
          image             = module.common.aoc_image
          image_pull_policy = "Always"
          args = [
            "--config",
          "/etc/eks/prometheus/config-all.yaml"]
          env {
            name  = "AWS_REGION"
            value = var.region
          }
          env {
            name  = "OTEL_RESOURCE_ATTRIBUTES"
            value = "ClusterName=${var.eks_cluster_name}"
          }
          resources {
            requests {
              cpu    = "0.2"
              memory = "256Mi"
            }
          }
        }
      }
    }
  }
}

module "demo_nginx" {
  count  = var.aoc_base_scenario == "prometheus" ? 1 : 0
  source = "./nginx"

  kubeconfig = local_file.kubeconfig.filename
  testing_id = module.common.testing_id
}

module "demo_appmesh" {
  count  = var.aoc_base_scenario == "prometheus" ? 1 : 0
  source = "./appmesh"

  kubeconfig            = local_file.kubeconfig.filename
  provider_url          = data.aws_eks_cluster.testing_cluster.identity[0].oidc[0].issuer
  region                = var.region
  sample_app_image_repo = var.sample_app_image_repo
  testing_id            = module.common.testing_id
}

module "demo_jmx" {
  count  = var.aoc_base_scenario == "prometheus" ? 1 : 0
  source = "./jmx"

  sample_app_image_repo = var.sample_app_image_repo
  testing_id            = module.common.testing_id
}

module "demo_memcached" {
  count = var.aoc_base_scenario == "prometheus" ? 1 : 0
  // source folder name cannot be the same as the chart name: https://github.com/hashicorp/terraform-provider-helm/issues/509
  source = "./memcached-service"

  testing_id = module.common.testing_id
}

module "demo_haproxy" {
  count = var.aoc_base_scenario == "prometheus" ? 1 : 0
  // source folder name cannot be the same as the chart name: https://github.com/hashicorp/terraform-provider-helm/issues/509
  source = "./haproxy"

  testing_id = module.common.testing_id
}