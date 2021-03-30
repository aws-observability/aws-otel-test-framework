#!/usr/bin/env bash

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

KUBECONFIG=${KUBECONFIG:-"~/.kube/config"}
NAMESPACE=${NAMESPACE:-""}
SERVICE_NAME=${SERVICE_NAME:-""}

service_wait() {
  timeout=60
  until [[ $timeout -eq 0 ]]; do
    external_ip=$(kubectl --kubeconfig=$KUBECONFIG get service -n$NAMESPACE $SERVICE_NAME --no-headers | awk {'print $4'})
    if [ -z "$external_ip" ]; then
      sleep 1
      timeout=$(( timeout - 1 ))
    else
      echo "get external ip $external_ip"
      return 0
    fi
  done
  echo "timeout wait external ip"
  return 1
}

service_wait
