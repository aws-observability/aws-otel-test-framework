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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.aoc.callers.HttpCaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.CWMetricHelper;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.SampleAppResponse;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.Metric;
import java.util.List;
import org.junit.Test;

/** this class covers the tests for CWMetricValidator. */
public class CWMetricValidatorTest {
  private final CWMetricHelper cwMetricHelper = new CWMetricHelper();

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
    Context context = initContext();
    List<Metric> metrics =
        cwMetricHelper.listExpectedMetrics(
            context, validationConfig.getExpectedMetricTemplate(), null);

    runValidation(validationConfig, context, metrics);
  }

  /**
   * test validation with enum template.
   *
   * @throws Exception when test fails
   */
  @Test
  public void testValidationSucceed() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedMetricTemplate("DEFAULT_EXPECTED_METRIC");
    Context context = initContext();

    // fake and mock a cloudwatch service
    List<Metric> mockedActualMetrics =
        cwMetricHelper.listExpectedMetrics(
            context, validationConfig.getExpectedMetricTemplate(), null);

    runValidation(validationConfig, context, mockedActualMetrics);
  }

  @Test
  public void testValidationFailedExpectedMetricMissing() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedMetricTemplate("DEFAULT_EXPECTED_METRIC");
    Context context = initContext();

    List<Metric> mockedActualMetrics =
        cwMetricHelper.listExpectedMetrics(
            context, validationConfig.getExpectedMetricTemplate(), null);

    // remove a mocked metric to ensure a test is failed
    mockedActualMetrics.remove(mockedActualMetrics.size() - 1);

    BaseException e =
        assertThrows(
            BaseException.class,
            () -> runValidation(validationConfig, context, mockedActualMetrics));
    assertEquals(e.getCode(), ExceptionCode.EXPECTED_METRIC_NOT_FOUND.getCode());
  }

  @Test
  public void testValidationFailedUnexpectedMetricFound() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedMetricTemplate("DEFAULT_EXPECTED_METRIC");
    Context context = initContext();

    List<Metric> mockedActualMetrics =
        cwMetricHelper.listExpectedMetrics(
            context, validationConfig.getExpectedMetricTemplate(), null);

    Metric fakeMetric = new Metric().withMetricName("fake metric").withNamespace("fake/namespace");
    mockedActualMetrics.add(fakeMetric);

    BaseException e =
        assertThrows(
            BaseException.class,
            () -> runValidation(validationConfig, context, mockedActualMetrics));
    assertEquals(e.getCode(), ExceptionCode.UNEXPECTED_METRIC_FOUND.getCode());
  }

  @Test
  public void testValidationIgnoreEmptyDimensions() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedMetricTemplate("DEFAULT_EXPECTED_METRIC");
    Context context = initContext();
    context.getCloudWatchContext().setIgnoreEmptyDimSet(true);

    List<Metric> mockedActualMetrics =
        cwMetricHelper.listExpectedMetrics(
            context, validationConfig.getExpectedMetricTemplate(), null);

    Metric fakeMetricNoDimensions =
        new Metric().withMetricName("fake metric").withNamespace("fake/namespace");
    mockedActualMetrics.add(fakeMetricNoDimensions);

    runValidation(validationConfig, context, mockedActualMetrics);
  }

  private Context initContext() {
    // fake vars
    String namespace = "fakednamespace";
    String testingId = "fakedTesingId";
    String region = "us-west-2";

    // faked context
    Context context = new Context(testingId, region, false, true);
    context.setMetricNamespace(namespace);
    context.setCloudWatchContext(new CloudWatchContext());
    context.getCloudWatchContext().setIgnoreEmptyDimSet(false);
    return context;
  }

  private void runValidation(
      ValidationConfig validationConfig, Context context, List<Metric> mockActualMetrics)
      throws Exception {
    // fake and mock a http caller
    String traceId = "fakedtraceid";
    HttpCaller httpCaller = mock(HttpCaller.class);
    SampleAppResponse sampleAppResponse = new SampleAppResponse();
    sampleAppResponse.setTraceId(traceId);
    when(httpCaller.callSampleApp()).thenReturn(sampleAppResponse);

    CloudWatchService cloudWatchService = mock(CloudWatchService.class);

    // mock listMetrics
    when(cloudWatchService.listMetrics(any(), any())).thenReturn(mockActualMetrics);

    // start validation
    CWMetricValidator validator = new CWMetricValidator();
    validator.init(
        context, validationConfig, httpCaller, validationConfig.getExpectedMetricTemplate());
    validator.setCloudWatchService(cloudWatchService);
    validator.setMaxRetryCount(1);
    validator.validate();
  }
}
