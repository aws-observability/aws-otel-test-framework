# creating a eks cluster takes around 10 minutes typically.
# so in the eks/k8s test, we need tester to provide the cluster instead of creating it in terraform
# so that we can shorten the execution time

module "common" {
  source = "../common"

  data_emitter_image = var.data_emitter_image
  aoc_image_repo = var.aoc_image_repo
  aoc_version = var.aoc_version
}

# region
provider "aws" {
  region  = var.region
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
  host = data.aws_eks_cluster.testing_cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.testing_cluster.certificate_authority[0].data)
  token = data.aws_eks_cluster_auth.testing_cluster.token
  load_config_file = false
  version = "~> 1.13"
}

# create a unique namespace for each run
resource "kubernetes_namespace" "aoc_ns" {
  metadata {
    name = "aoc-ns-${module.common.testing_id}"
  }
}

# load config into config map
data "template_file" "otconfig" {
  template = file(var.otconfig_path)

  vars = {
    region = var.region
    otel_service_namespace = module.common.otel_service_namespace
    otel_service_name = module.common.otel_service_name
    testing_id = module.common.testing_id
  }
}
resource "kubernetes_config_map" "aoc_config_map" {
  metadata {
    name = "otel-config"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }

  data = {
    "aoc-config.yml" = data.template_file.otconfig.rendered
  }
}

# deploy aoc and sample app
resource "kubernetes_deployment" "aoc_deployment" {
  metadata {
    name = "aoc"
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
        volume {
          name = "otel-config"
          config_map {
            name = kubernetes_config_map.aoc_config_map.metadata[0].name
          }
        }

        # aoc
        container {
          name = "aoc"
          image = module.common.aoc_image
          image_pull_policy = "Always"
          args = ["--config=/aoc/aoc-config.yml"]

          resources {
            requests {
              cpu = "0.2"
              memory = "256Mi"
            }
          }

          volume_mount {
            mount_path = "/aoc"
            name = "otel-config"
          }
        }

        # sample app
        container {
          name = "sample-app"
          image = var.data_emitter_image
          image_pull_policy = "Always"


          env {
            name = "OTEL_EXPORTER_OTLP_ENDPOINT"
            value = "127.0.0.1:55680"
          }

          env {
            name = "INSTANCE_ID"
            value = module.common.testing_id
          }

          env {
            name = "OTEL_RESOURCE_ATTRIBUTES"
            value = "service.namespace=${module.common.otel_service_namespace},service.name=${module.common.otel_service_name}"
          }

          env {
            name = "LISTEN_ADDRESS"
            value = "${module.common.sample_app_listen_address_ip}:${module.common.sample_app_listen_address_port}"
          }

          resources {
            requests {
              cpu = "0.2"
              memory = "256Mi"
            }

          }

          readiness_probe {
            http_get {
              path = "/"
              port = module.common.sample_app_listen_address_port
            }
            initial_delay_seconds = 10
            period_seconds = 5
          }
        }
      }
    }
  }
}

# create service upon the sample app
resource "kubernetes_service" "sample_app_service" {
  count = var.sample_app_callable ? 1 : 0
  metadata {
    name = "aoc"
    namespace = kubernetes_namespace.aoc_ns.metadata[0].name
  }
  spec {
    selector = {
      app = kubernetes_deployment.aoc_deployment.metadata[0].labels.app
    }

    type = "LoadBalancer"

    port {
      port = 80
      target_port = module.common.sample_app_listen_address_port
    }
  }
}

# run validator
resource "null_resource" "callable_sample_app_validator" {
  count = var.sample_app_callable ? 1 : 0
  provisioner "local-exec" {
    command = "${module.common.validator_path} --args='-c ${var.validation_config} -t ${module.common.testing_id} --region ${var.region} --metric-namespace ${module.common.otel_service_namespace}/${module.common.otel_service_name} --endpoint http://${kubernetes_service.sample_app_service[0].load_balancer_ingress.0.hostname}'"
    working_dir = "../../"
  }
}

# run validator
resource "null_resource" "validator" {
  count = !var.sample_app_callable ? 1 : 0
  provisioner "local-exec" {
    command = "${module.common.validator_path} --args='-c ${var.validation_config} -t ${module.common.testing_id} --region ${var.region} --metric-namespace ${module.common.otel_service_namespace}/${module.common.otel_service_name}'"
    working_dir = "../../"
  }
}





