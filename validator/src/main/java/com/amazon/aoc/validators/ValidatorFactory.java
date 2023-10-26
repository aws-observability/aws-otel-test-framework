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

package com.amazon.aoc.validators;

import com.amazon.aoc.callers.HttpCaller;
import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.TaskService;

public class ValidatorFactory {
  private Context context;

  public ValidatorFactory(Context context) {
    this.context = context;
  }

  /**
   * create and init validator base on config.
   *
   * @param validationConfig config from file
   * @return validator object
   * @throws Exception when there's no matched validator
   */
  public IValidator launchValidator(ValidationConfig validationConfig) throws Exception {
    // get validator
    IValidator validator;
    FileConfig expectedData = null;
    switch (validationConfig.getValidationType()) {
      case "trace":
        validator = new TraceValidator();
        expectedData = validationConfig.getExpectedTraceTemplate();
        break;
      case "loadbalancing":
        validator = new LoadBalancingValidator();
        break;
      case "cw-metric":
        validator = new CWMetricValidator();
        expectedData = validationConfig.getExpectedMetricTemplate();
        break;
      case "cw-logs":
        validator = new CWLogValidator();
        expectedData = validationConfig.getExpectedLogStructureTemplate();
        break;
      case "ecs-describe-task":
        validator = new ECSHealthCheckValidator(new TaskService(), 10);
        expectedData = validationConfig.getExpectedMetricTemplate();
        break;
      case "prom-static-metric":
        validator = new PrometheusStaticMetricValidator();
        expectedData = validationConfig.getExpectedMetricTemplate();
        break;
      case "prom-metric":
        validator = new PrometheusMetricValidator();
        expectedData = validationConfig.getExpectedMetricTemplate();
        break;
      case "alarm-pulling":
        validator = new AlarmPullingValidator();
        break;
      case "mocked-server":
        validator = new MockedServerValidator();
        break;
      case "performance":
        validator = new PerformanceValidator();
        break;
      case "container-insight-eks-prometheus-metrics":
      case "container-insight-ecs-prometheus-metrics":
        validator = new ContainerInsightPrometheusMetricsValidator();
        expectedData = validationConfig.getExpectedMetricTemplate();
        break;
      case "container-insight-eks-prometheus-logs":
        validator = new ContainerInsightPrometheusStructuredLogValidator();
        expectedData = validationConfig.getExpectedLogStructureTemplate();
        break;
      case "container-insight-eks-containerd-logs":
        validator = new ContainerInsightStructuredLogValidatorContainerd();
        expectedData = validationConfig.getExpectedLogStructureTemplate();
        break;
      case "container-insight-eks-docker-logs":
        validator = new ContainerInsightStructuredLogValidatorDocker();
        expectedData = validationConfig.getExpectedLogStructureTemplate();
        break;
      case "container-insight-ecs-logs":
        validator = new ConatinerInsightECSStructuredLogValidator();
        expectedData = validationConfig.getExpectedLogStructureTemplate();
        break;
      case "container-insight-ecs-prometheus-logs":
        validator = new ContainerIInsightECSPrometheusStructuredLogValidator();
        expectedData = validationConfig.getExpectedLogStructureTemplate();
        break;
      default:
        throw new BaseException(ExceptionCode.VALIDATION_TYPE_NOT_EXISTED);
    }

    // get caller
    ICaller caller;
    switch (validationConfig.getCallingType()) {
      case "http":
        caller = new HttpCaller(context.getEndpoint(), validationConfig.getHttpPath());
        break;
      case "none":
        caller = null;
        break;
      default:
        throw new BaseException(ExceptionCode.CALLER_TYPE_NOT_EXISTED);
    }

    // init validator
    validator.init(this.context, validationConfig, caller, expectedData);
    return validator;
  }
}
