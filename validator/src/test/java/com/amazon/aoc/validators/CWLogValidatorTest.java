package com.amazon.aoc.validators;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.aoc.callers.HttpCaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.SampleAppResponse;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.logs.model.OutputLogEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class CWLogValidatorTest {
  @Test
  public void testSuccessfullLogMessage() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedLogStructureTemplate(
        "file://"
            + System.getProperty("user.dir")
            + "/src/main/resources/expected-data-template/otlp/otlp-log.json");
    Context context = initContext();

    String message =
        "{\n"
            + "    \"body\": \"Executing outgoing-http-call\",\n"
            + "    \"severity_number\": 9,\n"
            + "    \"severity_text\": \"INFO\",\n"
            + "    \"flags\": 1,\n"
            + "    \"trace_id\": \"6541324dc3026f11c99345889da1a47d\",\n"
            + "    \"span_id\": \"c6f853c5f487c5e6\",\n"
            + "    \"resource\": {\n"
            + "        \"service.name\": \"aws-otel-integ-test\"}}";

    OutputLogEvent outputLogEvent = new OutputLogEvent();
    outputLogEvent.setMessage(message);
    List<OutputLogEvent> eventList = new ArrayList<>();
    eventList.add(outputLogEvent);
    runValidation(validationConfig, context, eventList);
  }

  @Test
  public void testFailedNoTraceId() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedLogStructureTemplate(
        "file://"
            + System.getProperty("user.dir")
            + "/src/main/resources/expected-data-template/otlp/otlp-log.json");
    Context context = initContext();

    String message =
        "{\n"
            + "    \"body\": \"Executing outgoing-http-call\",\n"
            + "    \"severity_number\": 9,\n"
            + "    \"severity_text\": \"INFO\",\n"
            + "    \"flags\": 1,\n"
            + "    \"span_id\": \"c6f853c5f487c5e6\",\n"
            + "    \"resource\": {\n"
            + "        \"service.name\": \"aws-otel-integ-test\"}}";

    OutputLogEvent outputLogEvent = new OutputLogEvent();
    outputLogEvent.setMessage(message);
    List<OutputLogEvent> eventList = new ArrayList<>();
    eventList.add(outputLogEvent);
    BaseException e =
        assertThrows(
            BaseException.class, () -> runValidation(validationConfig, context, eventList));
    assertEquals(e.getCode(), ExceptionCode.EXPECTED_LOG_NOT_FOUND.getCode());
  }

  @Test
  public void testFailedNullReport() throws Exception {
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedLogStructureTemplate(
        "file://"
            + System.getProperty("user.dir")
            + "/src/main/resources/expected-data-template/otlp/otlp-log.json");
    Context context = initContext();

    String message =
        "{\n"
            + "    \"body\": \"bad-body\",\n"
            + "    \"severity_number\": 9,\n"
            + "    \"severity_text\": \"INFO\",\n"
            + "    \"flags\": 1,\n"
            + "    \"trace_id\": \"6541324dc3026f11c99345889da1a47d\",\n"
            + "    \"span_id\": \"c6f853c5f487c5e6\",\n"
            + "    \"resource\": {\n"
            + "        \"service.name\": \"aws-otel-integ-test\"}}";

    OutputLogEvent outputLogEvent = new OutputLogEvent();
    outputLogEvent.setMessage(message);
    List<OutputLogEvent> eventList = new ArrayList<>();
    eventList.add(outputLogEvent);
    BaseException e =
        assertThrows(
            BaseException.class, () -> runValidation(validationConfig, context, eventList));
    assertEquals(e.getCode(), ExceptionCode.EXPECTED_LOG_NOT_FOUND.getCode());
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

  private CWLogValidator runValidation(
      ValidationConfig validationConfig, Context context, List<OutputLogEvent> mockActualEvents)
      throws Exception {
    // fake and mock a http caller
    String traceId = "fakedtraceid";
    HttpCaller httpCaller = mock(HttpCaller.class);
    SampleAppResponse sampleAppResponse = new SampleAppResponse();
    sampleAppResponse.setTraceId(traceId);
    when(httpCaller.callSampleApp()).thenReturn(sampleAppResponse);

    CloudWatchService cloudWatchService = mock(CloudWatchService.class);

    when(cloudWatchService.getLogs(any(), any(), anyLong(), anyInt())).thenReturn(mockActualEvents);

    // start validation
    CWLogValidator validator = new CWLogValidator();
    validator.init(
        context, validationConfig, httpCaller, validationConfig.getExpectedLogStructureTemplate());
    validator.setCloudWatchService(cloudWatchService);
    validator.setMaxRetryCount(1);
    validator.validate();
    return validator;
  }
}
