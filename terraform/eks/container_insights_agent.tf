data "template_file" "cluster_role_file" {
  template = file("./container-insights-agent/cluster_role.tpl")
  vars = {
    NAMESPACE = kubernetes_namespace.aoc_ns.metadata[0].name
  }
}

data "template_file" "cluster_role_binding_file" {
  template = file("./container-insights-agent/cluster_role_binding.tpl")
  vars = {
    NAMESPACE       = kubernetes_namespace.aoc_ns.metadata[0].name,
    SERVICE_ACCOUNT = kubernetes_service_account.aoc-agent-role.metadata[0].name
  }
}

data "template_file" "config_map_file" {
  template = file("./container-insights-agent/config_map.tpl")
  vars = {
    NAMESPACE = kubernetes_namespace.aoc_ns.metadata[0].name
  }
}

data "template_file" "daemonset_file" {
  template = file("./container-insights-agent/daemonset.tpl")
  vars = {
    NAMESPACE       = kubernetes_namespace.aoc_ns.metadata[0].name
    SERVICE_ACCOUNT = kubernetes_service_account.aoc-agent-role.metadata[0].name
    OTELIMAGE       = module.common.aoc_image
    REGION          = var.region
  }
}

resource "kubectl_manifest" "cluster_role" {
  yaml_body = data.template_file.cluster_role_file.rendered
}

resource "kubectl_manifest" "cluster_role_binding" {
  yaml_body = data.template_file.cluster_role_binding_file.rendered
  depends_on = [
    kubectl_manifest.cluster_role
  ]
}

resource "kubectl_manifest" "config_map" {
  yaml_body = data.template_file.config_map_file.rendered
}

resource "kubectl_manifest" "daemonset" {
  yaml_body = data.template_file.daemonset_file.rendered
  depends_on = [
    kubectl_manifest.config_map
  ]
}