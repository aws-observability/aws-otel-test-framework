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

import java.net.URL;

/**
 * PredefinedExpectedTemplate includes all the built-in expected data templates,
 * which are under resources/expected-data-templates.
 */
public enum PredefinedExpectedTemplate implements FileConfig {
  /**
   * metric template, defined in resources.
   */
  AMP_EXPECTED_METRIC("/expected-data-template/ampExpectedMetric.mustache"),
  OTLP_TO_AMP_EXPECTED_METRIC("/expected-data-template/otlpToAMPExpectedMetric.mustache"),
  DEFAULT_EXPECTED_METRIC("/expected-data-template/defaultExpectedMetric.mustache"),
  ENHANCED_EXPECTED_METRIC("/expected-data-template/enhancedExpectedMetric.mustache"),
  STATSD_EXPECTED_METRIC("/expected-data-template/statsdExpectedMetric.mustache"),
  ECS_CONTAINER_EXPECTED_METRIC("/expected-data-template/ecsContainerExpectedMetric.mustache"),
  CONTAINER_INSIGHT_EKS_PROMETHEUS_METRIC(
    "/expected-data-template/container-insight/eks/prometheus"),
  CONTAINER_INSIGHT_ECS_PROMETHEUS_METRIC(
      "/expected-data-template/container-insight/ecs/prometheus"),

  /**
   * trace template, defined in resources.
   */
  // not use default expected trace any more
  // DEFAULT_EXPECTED_TRACE("/expected-data-template/defaultExpectedTrace.mustache"),
  OTEL_SDK_AWSSDK_EXPECTED_TRACE("/expected-data-template/otelSDKexpectedAWSSDKTrace.mustache"),
  OTEL_SDK_HTTP_EXPECTED_TRACE("/expected-data-template/otelSDKexpectedHTTPTrace.mustache"),
  XRAY_SDK_AWSSDK_EXPECTED_TRACE("/expected-data-template/xraySDKexpectedAWSSDKTrace.mustache"),
  XRAY_SDK_HTTP_EXPECTED_TRACE("/expected-data-template/xraySDKexpectedHTTPTrace.mustache"),
  SPARK_SDK_HTTP_EXPECTED_TRACE("/expected-data-template/spark/sparkAppExpectedHTTPTrace.mustache"),
  SPARK_SDK_AWSSDK_EXPECTED_TRACE(
    "/expected-data-template/spark/sparkAppExpectedAWSSDKTrace.mustache"),
  SPARK_SDK_EC2_EXPECTED_TRACE("/expected-data-template/spark/sparkAppExpectedEC2Trace.mustache"),
  SPARK_SDK_ECS_EXPECTED_TRACE("/expected-data-template/spark/sparkAppExpectedECSTrace.mustache"),
  SPARK_SDK_EKS_EXPECTED_TRACE("/expected-data-template/spark/sparkAppExpectedEKSTrace.mustache"),
  LAMBDA_EXPECTED_TRACE("/expected-data-template/lambdaExpectedTrace.mustache"),
  SPRINGBOOT_SDK_HTTP_EXPECTED_TRACE(
    "/expected-data-template/springboot/springbootAppExpectedHTTPTrace.mustache"),
  SPRINGBOOT_SDK_AWSSDK_EXPECTED_TRACE(
    "/expected-data-template/springboot/springbootAppExpectedAWSSDKTrace.mustache"),
  GO_SDK_HTTP_EXPECTED_TRACE(
    "/expected-data-template/go/goAppExpectedHTTPTrace.mustache"
  ),
  GO_SDK_AWSSDK_EXPECTED_TRACE(
    "/expected-data-template/go/goAppExpectedAWSSDKTrace.mustache"
  ),
  JS_SDK_HTTP_EXPECTED_TRACE(
    "/expected-data-template/js/jsAppExpectedHTTPTrace.mustache"
  ),
  JS_SDK_AWSSDK_EXPECTED_TRACE(
    "/expected-data-template/js/jsAppExpectedAWSSDKTrace.mustache"
  ),

  /**
   * Log structure template, defined in resources.
   */
  CONTAINER_INSIGHT_EKS_PROMETHEUS_LOG(
    "/expected-data-template/container-insight/eks/prometheus"),
  CONTAINER_INSIGHT_EKS_LOG(
    "/expected-data-template/container-insight/eks/infrastructure"),
  CONTAINER_INSIGHT_ECS_LOG(
    "/expected-data-template/container-insight/ecs/ecs-instance"),
  CONTAINER_INSIGHT_ECS_PROMETHEUS_LOG(
      "/expected-data-template/container-insight/ecs/prometheus"),
  ;

  private String path;

  PredefinedExpectedTemplate(String path) {
    this.path = path;
  }

  @Override
  public URL getPath() {
    return getClass().getResource(path);
  }

}
