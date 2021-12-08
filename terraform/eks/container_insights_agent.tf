data "template_file" "cluster_role_file" {
  template = file("./container-insights-agent/cluster_role.tpl")
  vars = {
    NAMESPACE = var.deployment_type == "fargate" ? "default" : kubernetes_namespace.aoc_ns.metadata[0].name
  }
}

data "template_file" "cluster_role_binding_file" {
  template = file("./container-insights-agent/cluster_role_binding.tpl")
  vars = {
    NAMESPACE       = var.deployment_type == "fargate" ? "default" : kubernetes_namespace.aoc_ns.metadata[0].name,
    SERVICE_ACCOUNT = kubernetes_service_account.aoc-agent-role.metadata[0].name
  }
}

data "template_file" "config_map_file" {
  template = file("./container-insights-agent/config_map.tpl")
  vars = {
    NAMESPACE = var.deployment_type == "fargate" ? "default" : kubernetes_namespace.aoc_ns.metadata[0].name
  }
}

data "template_file" "daemonset_file" {
  template = file("./container-insights-agent/daemonset.tpl")
  vars = {
    NAMESPACE       = var.deployment_type == "fargate" ? "default" : kubernetes_namespace.aoc_ns.metadata[0].name
    SERVICE_ACCOUNT = kubernetes_service_account.aoc-agent-role.metadata[0].name
    OTELIMAGE       = module.common.aoc_image
    REGION          = var.region
  }
}

resource "kubectl_manifest" "service_account" {
  count = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body = templatefile("./container-insights-agent/service_account_fargate.tpl",
    {
      RoleArn : module.iam_assumable_role_admin.iam_role_arn
  })
  depends_on = [
    module.iam_assumable_role_admin
  ]
}

resource "kubectl_manifest" "cluster_role" {
  yaml_body = var.deployment_type == "fargate" ? file("./container-insights-agent/cluster_role_fargate.yml") : data.template_file.cluster_role_file.rendered
}

resource "kubectl_manifest" "cluster_role_binding" {
  yaml_body = var.deployment_type == "fargate" ? file("./container-insights-agent/cluster_role_binding_fargate.yml") : data.template_file.cluster_role_binding_file.rendered
  depends_on = [
    kubectl_manifest.cluster_role
  ]
}

resource "kubectl_manifest" "config_map" {
  yaml_body = var.deployment_type == "fargate" ? file("./container-insights-agent/config_map_fargate.yml") : data.template_file.config_map_file.rendered
}

resource "kubectl_manifest" "daemonset" {
  count = var.aoc_base_scenario == "infra" && var.deployment_type != "fargate" ? 1 : 0

  yaml_body = data.template_file.daemonset_file.rendered
  depends_on = [
    kubectl_manifest.config_map
  ]
}

resource "kubectl_manifest" "aoc_service_deploy" {
  count     = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body = file("./container-insights-agent/aoc_service_fargate.yml")
  depends_on = [
    kubectl_manifest.config_map
  ]
}

resource "kubectl_manifest" "aoc_fargate_deploy" {
  count = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body = templatefile("./container-insights-agent/stateful_set_fargate.tpl",
  { ClusterName : var.eks_cluster_name, AocRepo : var.aoc_image_repo, AocTag : var.aoc_version })
  depends_on = [
    kubectl_manifest.aoc_service_deploy
  ]
}

resource "kubectl_manifest" "logs_sample_fargate_deploy" {
  count     = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body = file("./container-insights-agent/logs_sample_fargate.yml")
  depends_on = [
    kubectl_manifest.aoc_fargate_deploy
  ]
}