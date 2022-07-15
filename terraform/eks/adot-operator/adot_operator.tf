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

variable "testing_id" {
  type    = string
  default = ""
}

variable "kubeconfig" {
  type    = string
  default = "kubeconfig"
}

variable "operator_repository" {
  default = "ghcr.io/open-telemetry/opentelemetry-operator/opentelemetry-operator"
}

variable "operator_tag" {
  default = "latest"
}

resource "helm_release" "adot-operator" {
  name = "adot-operator-${var.testing_id}"

  repository = "https://open-telemetry.github.io/opentelemetry-helm-charts"
  chart      = "opentelemetry-operator"

  values = [
    file("./adot-operator/adot-operator-values.yaml")
  ]

  set {
    name  = "manager.image.repository"
    value = var.operator_repository
  }

  set {
    name  = "manager.image.tag"
    value = var.operator_tag
  }

  provisioner "local-exec" {
    command = "kubectl wait --kubeconfig=${var.kubeconfig} --timeout=5m --for=condition=available deployment opentelemetry-operator-controller-manager"
  }
}
