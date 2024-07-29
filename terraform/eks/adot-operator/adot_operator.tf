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
}

variable "operator_tag" {
}

variable "aoc_image_repo" {
}

variable "aoc_version" {
}

resource "helm_release" "adot-operator" {
  name = "adot-operator-${var.testing_id}"

  repository = "https://open-telemetry.github.io/opentelemetry-helm-charts"
  chart      = "opentelemetry-operator"
  version   = "0.63.2"
  wait = true
  timeout = 600

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

  set {
    name  = "manager.collectorImage.tag"
    value = var.aoc_version
  }

  set {
    name  = "manager.collectorImage.repository"
    value = var.aoc_image_repo
  }

  set {
    name  = "admissionWebhooks.certManager.enabled"
    value = true
  }
}
