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

resource "helm_release" "adot-operator" {
  name = "adot-operator-${var.testing_id}"
  // currently adot operator is at 0.41.0
  // helm charts >-0.5.4 require 0.43.0
  repository = "https://open-telemetry.github.io/opentelemetry-helm-charts"
  chart      = "opentelemetry-operator"

  values = [
    file("./adot-operator/adot-operator-values.yaml")
  ]

  provisioner "local-exec" {
    command = "kubectl wait --kubeconfig=${var.kubeconfig} --timeout=5m --for=condition=available deployment opentelemetry-operator-controller-manager"
  }
}
