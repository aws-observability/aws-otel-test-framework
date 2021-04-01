/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.opentelemetry.trace.model.topology;

import java.util.ArrayList;
import java.util.List;

public class Topology {

  public List<ServiceTier> services = new ArrayList<>();

  public ServiceTier getServiceTier(String serviceName) {
    return this.services.stream().filter(s -> s.serviceName.equalsIgnoreCase(serviceName))
        .findFirst().get();
  }
}
