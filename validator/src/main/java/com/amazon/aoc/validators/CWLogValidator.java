package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.logs.model.OutputLogEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ListReportProvider;
import com.github.fge.jsonschema.report.LogLevel;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CWLogValidator implements IValidator {

  protected String logStreamName = "otlp-logs";

  private static final String LOGGROUPPATH = "/aws/ecs/otlp/%s/logs";

  protected CloudWatchService cloudWatchService;
  private static final int CHECK_INTERVAL_IN_MILLI = 30 * 1000;
  private static final int CHECK_DURATION_IN_SECONDS = 2 * 60;
  private static final int MAX_RETRY_COUNT = 12;
  private static final int QUERY_LIMIT = 100;
  private JsonSchema schema;
  protected String logGroupName;

  private ICaller caller;

  private Context context;

  protected final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedDataTemplate)
      throws Exception {
    this.context = context;
    this.caller = caller;
    cloudWatchService = new CloudWatchService(context.getRegion());
    logGroupName = String.format(LOGGROUPPATH, context.getTestingId());
    MustacheHelper mustacheHelper = new MustacheHelper();
    String templateInput = mustacheHelper.render(expectedDataTemplate, context);
    JsonNode jsonNode = JsonLoader.fromString(templateInput);
    JsonSchemaFactory jsonSchemaFactory =
        JsonSchemaFactory.newBuilder()
            .setReportProvider(new ListReportProvider(LogLevel.INFO, LogLevel.FATAL))
            .freeze();
    JsonSchema jsonSchema = jsonSchemaFactory.getJsonSchema(jsonNode);
    this.schema = jsonSchema;
  }

  @Override
  public void validate() throws Exception {
    caller.callSampleApp();
    RetryHelper.retry(
        getMaxRetryCount(),
        CHECK_INTERVAL_IN_MILLI,
        true,
        () -> {
          Instant startTime =
              Instant.now().minusSeconds(CHECK_DURATION_IN_SECONDS).truncatedTo(ChronoUnit.MINUTES);
          fetchAndValidateLogs(startTime);
        });
  }

  protected void fetchAndValidateLogs(Instant startTime) throws Exception {
    List<OutputLogEvent> logEvents =
        cloudWatchService.getLogs(
            logGroupName, logStreamName, startTime.toEpochMilli(), QUERY_LIMIT);
    if (logEvents.isEmpty()) {
      throw new BaseException(
          ExceptionCode.LOG_FORMAT_NOT_MATCHED,
          String.format(
              "[StructuredLogValidator] no logs found under log stream %s" + " in log group %s",
              logStreamName, logGroupName));
    }
    for (OutputLogEvent logEvent : logEvents) {
      if (logEvent.getMessage().contains("Executing outgoing-http-call")) {
        validateJsonSchema(logEvent.getMessage());
      }
    }
  }

  protected void validateJsonSchema(String logEventMsg) throws Exception {
    JsonNode logEventNode = mapper.readTree(logEventMsg);
    if (schema != null) {
      ProcessingReport report = schema.validate(JsonLoader.fromString(logEventNode.toString()));
      if (report.isSuccess()) {
        log.info("Report was a success");
      } else {
        log.info("[StructuredLogValidator] failed to validate schema \n");
        log.info(report.toString() + "\n");
      }
    }
  }

  protected int getMaxRetryCount() {
    return MAX_RETRY_COUNT;
  }
}