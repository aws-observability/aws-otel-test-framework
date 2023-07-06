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
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.SampleAppResponse;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.XRayService;
import com.amazonaws.services.xray.model.Trace;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

/** this class covers the tests for LoadBalanceValidator. */
public class LoadBalanceValidatorTest {

  /** test validation when it succeeds */
  @Test
  public void testValidationSucceeds() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    Context context = initContext();

    String mockedTrace =
        "[{ \"segments\": [{ \"document\": '{ \"metadata\": { \"default\": {\"collector-id\": 3} }, \"subsegments\": [{ \"metadata\": { \"default\": {\"collector-id\": 3} }}, { \"metadata\": { \"default\": {\"collector-id\": 3} }}] }' }] }]";

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<Trace> mockedTraceList =
        mapper.readValue(
            mockedTrace.getBytes(StandardCharsets.UTF_8), new TypeReference<List<Trace>>() {});

    runValidation(validationConfig, context, mockedTraceList);
  }

  /** test validation when fails due to difference collector ids */
  @Test
  public void testValidationFailedExpectedMatchingId() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    Context context = initContext();

    String mockedTrace =
        "[{ \"segments\": [{ \"document\": '{ \"metadata\": { \"default\": {\"collector-id\": 3} }, \"subsegments\": [{ \"metadata\": { \"default\": {\"collector-id\": 2} }}, { \"metadata\": { \"default\": {\"collector-id\": 3} }}] }' }] }]";

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<Trace> mockedTraceList =
        mapper.readValue(
            mockedTrace.getBytes(StandardCharsets.UTF_8), new TypeReference<List<Trace>>() {});

    BaseException e =
        assertThrows(
            BaseException.class, () -> runValidation(validationConfig, context, mockedTraceList));
    assertEquals(e.getCode(), ExceptionCode.COLLECTOR_ID_NOT_MATCHED.getCode());
  }

  /** test validation when it fails due to not enough spans for the test */
  @Test
  public void testValidationFailedExpectedMoreSpans() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    Context context = initContext();

    String mockedTrace =
        "[{ \"segments\": [{ \"document\": '{ \"metadata\": { \"default\": {\"collector-id\": 3} }, \"subsegments\": [] }' }] }]";

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<Trace> mockedTraceList =
        mapper.readValue(
            mockedTrace.getBytes(StandardCharsets.UTF_8), new TypeReference<List<Trace>>() {});

    BaseException e =
        assertThrows(
            BaseException.class, () -> runValidation(validationConfig, context, mockedTraceList));
    assertEquals(e.getCode(), ExceptionCode.NOT_ENOUGH_SPANS.getCode());
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
      ValidationConfig validationConfig, Context context, List<Trace> mockActualTrace)
      throws Exception {
    // fake and mock a http caller
    String traceId = "fakedtraceid";
    HttpCaller httpCaller = mock(HttpCaller.class);
    SampleAppResponse sampleAppResponse = new SampleAppResponse();
    sampleAppResponse.setTraceId(traceId);
    when(httpCaller.callSampleApp()).thenReturn(sampleAppResponse);

    XRayService xrayService = mock(XRayService.class);
    when(xrayService.listTraceByIds(any())).thenReturn(mockActualTrace);

    // start validation
    LoadBalanceValidator validator = new LoadBalanceValidator();
    validator.init(
        context, validationConfig, httpCaller, validationConfig.getExpectedTraceTemplate());
    validator.setXRayService(xrayService);
    validator.validate();
  }
}
