data "template_file" "cluster_role_file" {
  template = file("./container-insights-agent/cluster_role.tpl")
  vars = {
    NAMESPACE = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
  }
  depends_on = [aws_eks_fargate_profile.test_profile]
}

data "template_file" "cluster_role_binding_file" {
  template = file("./container-insights-agent/cluster_role_binding.tpl")
  vars = {
    NAMESPACE       = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name,
    SERVICE_ACCOUNT = kubernetes_service_account.aoc-agent-role.metadata[0].name
  }
  depends_on = [aws_eks_fargate_profile.test_profile]
}

data "template_file" "config_map_file" {
  template = file("./container-insights-agent/config_map.tpl")
  vars = {
    NAMESPACE = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
  }
  depends_on = [aws_eks_fargate_profile.test_profile]
}

data "template_file" "daemonset_file" {
  template = file("./container-insights-agent/daemonset.tpl")
  vars = {
    NAMESPACE       = var.deployment_type == "fargate" ? tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace : kubernetes_namespace.aoc_ns.metadata[0].name
    SERVICE_ACCOUNT = kubernetes_service_account.aoc-agent-role.metadata[0].name
    OTELIMAGE       = module.common.aoc_image
    REGION          = var.region
  }
  depends_on = [aws_eks_fargate_profile.test_profile]
}

resource "kubectl_manifest" "service_account" {
  count = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body = templatefile("./container-insights-agent/service_account_fargate.yml",
    {
      RoleArn : module.iam_assumable_role_admin.iam_role_arn,
      Namespace : tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace
  })
  depends_on = [
    module.iam_assumable_role_admin,
    aws_eks_fargate_profile.test_profile
  ]
}

resource "kubectl_manifest" "cluster_role" {
  yaml_body = var.deployment_type == "fargate" ? file("./container-insights-agent/cluster_role_fargate.yml") : data.template_file.cluster_role_file.rendered
}

resource "kubectl_manifest" "cluster_role_binding" {
  yaml_body = var.deployment_type == "fargate" ? templatefile("./container-insights-agent/cluster_role_binding_fargate.yml", { Namespace : tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace }) : data.template_file.cluster_role_binding_file.rendered
  depends_on = [
    kubectl_manifest.cluster_role,
    aws_eks_fargate_profile.test_profile
  ]
}

resource "kubectl_manifest" "config_map" {
  yaml_body  = var.deployment_type == "fargate" ? templatefile("./container-insights-agent/config_map_fargate.yml", { Namespace : tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace }) : data.template_file.config_map_file.rendered
  depends_on = [aws_eks_fargate_profile.test_profile]
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
  yaml_body = templatefile("./container-insights-agent/aoc_service_fargate.yml", { Namespace : tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace })
  depends_on = [
    kubectl_manifest.config_map,
    aws_eks_fargate_profile.test_profile
  ]
}

resource "kubectl_manifest" "aoc_fargate_deploy" {
  count = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body = templatefile("./container-insights-agent/stateful_set_fargate.yml",
  { ClusterName : var.eks_cluster_name, AocRepo : var.aoc_image_repo, AocTag : var.aoc_version, Namespace : tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace })
  depends_on = [
    kubectl_manifest.aoc_service_deploy,
    aws_eks_fargate_profile.test_profile
  ]
}

resource "kubectl_manifest" "logs_sample_fargate_deploy" {
  count     = var.aoc_base_scenario == "infra" && var.deployment_type == "fargate" ? 1 : 0
  yaml_body = templatefile("./container-insights-agent/logs_sample_fargate.yml", { Namespace : tolist(aws_eks_fargate_profile.test_profile.selector)[0].namespace })
  depends_on = [
    kubectl_manifest.aoc_fargate_deploy,
    aws_eks_fargate_profile.test_profile
  ]
}