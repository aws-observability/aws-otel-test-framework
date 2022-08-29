data "template_file" "cluster_role_file" {
  count    = 1
  template = file("./container-insights-agent/cluster_role.tpl")
  vars = {
    NAMESPACE = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
  }
}

data "template_file" "cluster_role_binding_file" {
  count    = 1
  template = file("./container-insights-agent/cluster_role_binding.tpl")
  vars = {
    NAMESPACE       = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name,
    SERVICE_ACCOUNT = kubernetes_service_account.aoc-agent-role[count.index].metadata[0].name
  }
}

data "template_file" "config_map_file" {
  count    = 1
  template = file("./container-insights-agent/config_map.tpl")
  vars = {
    NAMESPACE = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
  }
}

data "template_file" "daemonset_file" {
  count    = 1
  template = file("./container-insights-agent/daemonset.tpl")
  vars = {
    NAMESPACE       = var.deployment_type == "fargate" ? kubernetes_namespace.aoc_fargate_ns.metadata[0].name : kubernetes_namespace.aoc_ns.metadata[0].name
    SERVICE_ACCOUNT = kubernetes_service_account.aoc-agent-role[count.index].metadata[0].name
    OTELIMAGE       = module.common.aoc_image
    REGION          = var.region
  }
}

resource "kubectl_manifest" "service_account" {
  count = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body = templatefile("./container-insights-agent/service_account_fargate.yml",
    {
      RoleArn : module.iam_assumable_role_admin.iam_role_arn,
      Namespace : kubernetes_namespace.aoc_fargate_ns.metadata[0].name
  })
  depends_on = [module.iam_assumable_role_admin]
}

resource "kubectl_manifest" "cluster_role" {
  count     = 1
  yaml_body = var.deployment_type == "fargate" ? file("./container-insights-agent/cluster_role_fargate.yml") : data.template_file.cluster_role_file[count.index].rendered
}

resource "kubectl_manifest" "cluster_role_binding" {
  count      = 1
  yaml_body  = var.deployment_type == "fargate" ? templatefile("./container-insights-agent/cluster_role_binding_fargate.yml", { Namespace : kubernetes_namespace.aoc_fargate_ns.metadata[0].name }) : data.template_file.cluster_role_binding_file[count.index].rendered
  depends_on = [kubectl_manifest.cluster_role]
}

resource "kubectl_manifest" "config_map" {
  count = 1
  yaml_body = var.deployment_type == "fargate" ? templatefile("./container-insights-agent/config_map_fargate.yml",
    {
      Namespace : kubernetes_namespace.aoc_fargate_ns.metadata[0].name,
      TestingId : module.common.testing_id
  }) : data.template_file.config_map_file[count.index].rendered
}

resource "kubectl_manifest" "daemonset" {
  count = var.aoc_base_scenario == "infra" && var.deployment_type != "fargate" ? 1 : 0

  yaml_body = data.template_file.daemonset_file[count.index].rendered
  depends_on = [
    kubectl_manifest.config_map
  ]
}

resource "kubectl_manifest" "aoc_service_deploy" {
  count      = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body  = templatefile("./container-insights-agent/aoc_service_fargate.yml", { Namespace : kubernetes_namespace.aoc_fargate_ns.metadata[0].name })
  depends_on = [kubectl_manifest.config_map]
}

resource "kubectl_manifest" "aoc_fargate_deploy" {
  count = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body = templatefile("./container-insights-agent/stateful_set_fargate.yml",
  { ClusterName : var.eks_cluster_name, AocRepo : var.aoc_image_repo, AocTag : var.aoc_version, Namespace : kubernetes_namespace.aoc_fargate_ns.metadata[0].name })
  depends_on = [kubectl_manifest.aoc_service_deploy]
}

resource "kubectl_manifest" "logs_sample_fargate_deploy" {
  count      = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body  = templatefile("./container-insights-agent/logs_sample_fargate.yml", { Namespace : kubernetes_namespace.aoc_fargate_ns.metadata[0].name })
  depends_on = [kubectl_manifest.aoc_fargate_deploy]
}