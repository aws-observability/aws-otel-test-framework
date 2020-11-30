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
  String callingType = "none";

  String httpPath;
  String httpMethod;

  String expectedResultPath;
  Boolean shouldValidateMetricValue;

  ExpectedMetric expectedMetricTemplate;
  ExpectedTrace expectedTraceTemplate;

  /**
   * alarm related.
   */
  Integer pullingDuration;
  Integer pullingTimes;

  /**
   * performance test related.
   */
  String cpuMetricName;
  String memoryMetricName;
  Integer collectionPeriod;
  Integer datapointPeriod;
  String dataType;
  String dataMode;
  Integer dataRate;

  // Dimensions
  String testcase;
  String commitId;
  String instanceId;
  String instanceType;
  String launchDate;
  String exe;
  String processName;
  String testingAmi;
  String negativeSoaking;
}
