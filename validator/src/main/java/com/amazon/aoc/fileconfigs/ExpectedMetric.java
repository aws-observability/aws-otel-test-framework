/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazon.aoc.fileconfigs;

import lombok.Getter;

@Getter
public enum ExpectedMetric implements FileConfig {
  DEFAULT_EXPECTED_METRIC("/expected-data-template/defaultExpectedMetric.mustache"),
  ENHANCED_EXPECTED_METRIC("/expected-data-template/enhancedExpectedMetric.mustache"),
  STATSD_EXPECTED_METRIC("/expected-data-template/statsdExpectedMetric.mustache"),
  ECS_CONTAINER_EXPECTED_METRIC("/expected-data-template/ecsContainerExpectedMetric.mustache"),
  EKS_CONTAINER_INSIGHT_METRIC(
          "/expected-data-template/eksContainerInsightExpectedMetrics.mustache"),
  ;

  private String path;

  ExpectedMetric(String path) {
    this.path = path;
  }
}
