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

  protected CloudWatchService cloudWatchService;
  private static final int CHECK_INTERVAL_IN_MILLI = 30 * 1000;
  private static final int CHECK_DURATION_IN_SECONDS = 2 * 60;
  private static final int MAX_RETRY_COUNT = 12;
  private static final int QUERY_LIMIT = 100;
  private JsonSchema schema;
  protected String logGroupName;
  private Context context;

  protected final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedDataTemplate)
      throws Exception {
    log.info("CWLog init starting");
    this.context = context;
    cloudWatchService = new CloudWatchService(context.getRegion());
    logGroupName = String.format("otlp-receiver", context.getCloudWatchContext().getClusterName());
    log.info("CW Log group name is: " + logGroupName);
    //    cloudWatchService = new CloudWatchService(context.getRegion());
    MustacheHelper mustacheHelper = new MustacheHelper();
    String templateInput = mustacheHelper.render(expectedDataTemplate, context);
    JsonNode jsonNode = JsonLoader.fromString(templateInput);
    JsonSchemaFactory jsonSchemaFactory =
        JsonSchemaFactory.newBuilder()
            .setReportProvider(new ListReportProvider(LogLevel.INFO, LogLevel.FATAL))
            .freeze();
    JsonSchema schema = jsonSchemaFactory.getJsonSchema(jsonNode);
    this.schema = schema;
    log.info(("CWLog init ending"));
    caller.callSampleApp();
  }

  //  @Override
  //  public void init(
  //      Context context,
  //      ValidationConfig validationConfig,
  //      ICaller caller,
  //      FileConfig expectedDataTemplate)
  //      throws Exception {
  //    log.info("CWLog init starting");
  //    this.context = context;
  //    logGroupName = String.format("otlp-receiver",
  // context.getCloudWatchContext().getClusterName());
  //    cloudWatchService = new CloudWatchService(context.getRegion());
  //    MustacheHelper mustacheHelper = new MustacheHelper();
  //    String templateInput = mustacheHelper.render(expectedDataTemplate, context);
  //    JsonNode jsonNode = JsonLoader.fromString(templateInput);
  //    JsonSchemaFactory jsonSchemaFactory =
  //        JsonSchemaFactory.newBuilder()
  //            .setReportProvider(new ListReportProvider(LogLevel.INFO, LogLevel.FATAL))
  //            .freeze();
  //    JsonSchema schema = jsonSchemaFactory.getJsonSchema(jsonNode);
  //    this.schema = schema;
  //    log.info(("CWLog init ending"));
  //  }

  @Override
  public void validate() throws Exception {
    log.info(("CWLog validate starting"));
    RetryHelper.retry(
        getMaxRetryCount(),
        CHECK_INTERVAL_IN_MILLI,
        true,
        () -> {
          Instant startTime =
              Instant.now().minusSeconds(CHECK_DURATION_IN_SECONDS).truncatedTo(ChronoUnit.MINUTES);
          log.info("Start time is: " + startTime.toEpochMilli());
          fetchAndValidateLogs(startTime);
        });
  }

  protected void fetchAndValidateLogs(Instant startTime) throws Exception {
    log.info(("CWLog fetch starting"));
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
      log.info("Log message: " + logEvent.getMessage());
      validateJsonSchema(logEvent.getMessage());
    }
  }

  protected void validateJsonSchema(String logEventMsg) throws Exception {
    log.info("In validateJsonSchema");
    JsonNode logEventNode = mapper.readTree(logEventMsg);
    log.info("In validateJsonSchema - post readTree");
    if (schema != null) {
      log.info("In validateJsonSchema - schema isn't null");
      ProcessingReport report = schema.validate(JsonLoader.fromString(logEventNode.toString()));
      if (report.isSuccess()) {
        //        validatedSchema.add(key);
        log.info("Report was a success");
      } else {
        // This will probably generate a lot of extra logs
        // may want to log this to a different level in the future.
        log.info("[StructuredLogValidator] failed to validate schema \n");
        log.info(report.toString() + "\n");
      }
    }
  }

  protected int getMaxRetryCount() {
    return MAX_RETRY_COUNT;
  }
}
