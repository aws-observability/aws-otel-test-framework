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
import com.amazon.aoc.fileconfigs.LocalPathExpectedTemplate;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.SampleAppResponse;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.XRayService;
import com.amazonaws.services.xray.model.Trace;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/** this class covers the tests for LoadBalanceValidator. */
public class LoadBalancingValidatorTest {

  /** test validation when it succeeds */
  @Test
  public void testValidationSucceeds() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    Context context = initContext();
    MustacheHelper mustacheHelper = new MustacheHelper();

    String testFilePath =
        new File("./src/test/java/com/amazon/aoc/testdata/LoadBalancingSuccess.mustache")
            .getAbsolutePath();
    String mockedTrace =
        mustacheHelper.render(new LocalPathExpectedTemplate("file://" + testFilePath), null);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Trace mockedTraceList =
        mapper.readValue(
            mockedTrace.getBytes(StandardCharsets.UTF_8), new TypeReference<Trace>() {});

    runValidation(validationConfig, context, mockedTraceList);
  }

  /** test validation when it fails due to different collector ids */
  @Test
  public void testValidationFailedExpectedMatchingId() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    Context context = initContext();
    MustacheHelper mustacheHelper = new MustacheHelper();

    String testFilePath =
        new File("./src/test/java/com/amazon/aoc/testdata/LoadBalancingFailureMatchingIds.mustache")
            .getAbsolutePath();
    String mockedTrace =
        mustacheHelper.render(new LocalPathExpectedTemplate("file://" + testFilePath), null);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Trace mockedTraceList =
        mapper.readValue(
            mockedTrace.getBytes(StandardCharsets.UTF_8), new TypeReference<Trace>() {});

    BaseException e =
        assertThrows(
            BaseException.class, () -> runValidation(validationConfig, context, mockedTraceList));
    assertEquals(e.getCode(), ExceptionCode.COLLECTOR_ID_NOT_MATCHED.getCode());
  }

  @Test
  public void testValidationFailedExpectedMoreSpans() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    Context context = initContext();
    MustacheHelper mustacheHelper = new MustacheHelper();

    String testFilePath =
        new File("./src/test/java/com/amazon/aoc/testdata/LoadBalancingFailureSpanCount.mustache")
            .getAbsolutePath();
    String mockedTrace =
        mustacheHelper.render(new LocalPathExpectedTemplate("file://" + testFilePath), null);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Trace mockedTraceList =
        mapper.readValue(
            mockedTrace.getBytes(StandardCharsets.UTF_8), new TypeReference<Trace>() {});

    BaseException e =
        assertThrows(
            BaseException.class, () -> runValidation(validationConfig, context, mockedTraceList));
    assertEquals(e.getCode(), ExceptionCode.NOT_ENOUGH_SPANS.getCode());
  }

  private Context initContext() {
    // fake vars
    String testingId = "fakedTesingId";
    String region = "us-west-2";

    // faked context
    Context context = new Context(testingId, region, false, true);
    return context;
  }

  private void runValidation(
      ValidationConfig validationConfig, Context context, Trace mockActualTrace) throws Exception {
    // fake and mock a http caller
    String traceId = "fakedtraceid";
    HttpCaller httpCaller = mock(HttpCaller.class);
    SampleAppResponse sampleAppResponse = new SampleAppResponse();
    sampleAppResponse.setTraceId(traceId);
    when(httpCaller.callSampleApp()).thenReturn(sampleAppResponse);

    XRayService xrayService = mock(XRayService.class);
    when(xrayService.getTraceById(any())).thenReturn(mockActualTrace);

    // start validation
    LoadBalancingValidator validator = new LoadBalancingValidator();
    validator.init(
        context,
        validationConfig,
        httpCaller,
        validationConfig.getExpectedTraceTemplate(),
        xrayService);
    validator.validate();
  }
}
