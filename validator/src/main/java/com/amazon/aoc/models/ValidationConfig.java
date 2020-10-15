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

package com.amazon.aoc.models;

import com.amazon.aoc.fileconfigs.ExpectedMetric;
import com.amazon.aoc.fileconfigs.ExpectedTrace;
import lombok.Data;

@Data
public class ValidationConfig {
  String validationType;
  String callingType;

  String httpPath;
  String httpMethod;

  ExpectedMetric expectedMetricTemplate;
  ExpectedTrace expectedTraceTemplate;
}
