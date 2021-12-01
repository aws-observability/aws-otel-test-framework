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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.aoc.callers.HttpCaller;
import com.amazon.aoc.helpers.CWMetricHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.SampleAppResponse;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.Metric;
import org.junit.Test;

import java.util.List;



/**
 * this class covers the tests for CWMetricValidator.
 */
public class CWMetricValidatorTest {
  private CWMetricHelper cwMetricHelper = new CWMetricHelper();

  /**
   * test validation with local file path template file.
   *
   * @throws Exception when test fails
   */
  @Test
  public void testValidationSucceedWithCustomizedFilePath() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedMetricTemplate(
        "file://"
        + System.getProperty("user.dir")
        + "/src/main/resources/expected-data-template/defaultExpectedMetric.mustache");
    runValidation(validationConfig, initContext());
  }

  /**
   * test validation with enum template.
   * @throws Exception when test fails
   */
  @Test
  public void testValidationSucceed() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedMetricTemplate("DEFAULT_EXPECTED_METRIC");
    runValidation(validationConfig, initContext());
  }

  private Context initContext() {
    // fake vars
    String namespace = "fakednamespace";
    String testingId = "fakedTesingId";
    String region = "us-west-2";

    // faked context
    Context context = new Context(
        testingId,
        region,
        false,
            true
    );
    context.setMetricNamespace(namespace);
    return context;
  }

  private void runValidation(ValidationConfig validationConfig, Context context) throws Exception {
    // fake vars
    String traceId = "fakedtraceid";

    // fake and mock a http caller
    HttpCaller httpCaller = mock(HttpCaller.class);
    SampleAppResponse sampleAppResponse = new SampleAppResponse();
    sampleAppResponse.setTraceId(traceId);
    when(httpCaller.callSampleApp()).thenReturn(sampleAppResponse);

    // fake and mock a cloudwatch service
    List<Metric> metrics = cwMetricHelper.listExpectedMetrics(
        context,
        validationConfig.getExpectedMetricTemplate(),
        httpCaller
        );
    CloudWatchService cloudWatchService = mock(CloudWatchService.class);

    // mock listMetrics
    when(cloudWatchService.listMetrics(any(), any())).thenReturn(metrics);


    // start validation
    CWMetricValidator validator = new CWMetricValidator();
    validator.init(
        context,
        validationConfig,
        httpCaller,
        validationConfig.getExpectedMetricTemplate()
    );
    validator.setCloudWatchService(cloudWatchService);
    validator.setMaxRetryCount(1);
    validator.validate();
  }
}
