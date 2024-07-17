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

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.logs.model.OutputLogEvent;
import com.amazonaws.util.StringUtils;
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
public abstract class AbstractStructuredLogValidator implements IValidator {
  private static final int MAX_RETRY_COUNT = 6;
  private static final int QUERY_LIMIT = 500;
  private static final int CHECK_INTERVAL_IN_MILLI = 30 * 1000;
  private static final int CHECK_DURATION_IN_SECONDS = 2 * 60;

  protected Map<String, JsonSchema> schemasToValidate = new HashMap<>();
  protected Set<String> validatedSchema = new HashSet<>();
  protected Set<String> logStreamNames = new HashSet<>();
  protected String logGroupName;

  protected CloudWatchService cloudWatchService;
  protected final ObjectMapper mapper = new ObjectMapper();
  protected ICaller caller;

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedDataTemplate)
      throws Exception {
    cloudWatchService = new CloudWatchService(context.getRegion());
    this.caller = caller;
    init(context, expectedDataTemplate);

    if (StringUtils.isNullOrEmpty(logGroupName)) {
      throw new BaseException(
          ExceptionCode.VALIDATION_TYPE_NOT_EXISTED, "log group name cannot be null");
    }
    if (schemasToValidate.isEmpty()) {
      throw new BaseException(
          ExceptionCode.VALIDATION_TYPE_NOT_EXISTED, "json schema cannot be null");
    }
  }

  abstract void init(Context context, FileConfig expectedDataTemplate) throws Exception;

  abstract String getJsonSchemaMappingKey(JsonNode jsonNode);

  protected int getMaxRetryCount() {
    return MAX_RETRY_COUNT;
  }

  @Override
  public void validate() throws Exception {
    log.info("[StructuredLogValidator] start validating structured log");

    RetryHelper.retry(
        getMaxRetryCount(),
        CHECK_INTERVAL_IN_MILLI,
        true,
        () -> {
          Instant startTime =
              Instant.now().minusSeconds(CHECK_DURATION_IN_SECONDS).truncatedTo(ChronoUnit.MINUTES);
          fetchAndValidateLogs(startTime);
          checkResult();
        });
    log.info("[StructuredLogValidator] finish validation successfully");
  }

  protected void fetchAndValidateLogs(Instant startTime) throws Exception {
    for (String logStreamName : logStreamNames) {
      try {
        List<OutputLogEvent> logEvents =
            cloudWatchService.getLogs(
                logGroupName, logStreamName, startTime.toEpochMilli(), QUERY_LIMIT);

        if (logEvents.isEmpty()) {
          throw new BaseException(
              ExceptionCode.LOG_FORMAT_NOT_MATCHED,
              String.format(
                  "[StructuredLogValidator] no logs found under log stream %s" + "in log group %s",
                  logStreamName, logGroupName));
        }
        for (OutputLogEvent logEvent : logEvents) {
          validateJsonSchema(logEvent.getMessage());
        }
      } catch (AmazonClientException e) {
        log.info(
            String.format(
                "[StructuredLogValidator] failed to retrieve log stream %s" + "in log group %s",
                logStreamName, logGroupName));
        throw e;
      }
    }
  }

  private void checkResult() throws BaseException {
    if (schemasToValidate.isEmpty() || schemasToValidate.keySet().equals(validatedSchema)) {
      log.info(
          String.format(
              "[StructuredLogValidator] log structure validation successful"
                  + "isEmpty: %b isEqual: %b",
              schemasToValidate.isEmpty(), schemasToValidate.keySet().equals(validatedSchema)));
      return;
    }
    ArrayList<String> failedTargets = new ArrayList<>();
    for (String key : schemasToValidate.keySet()) {
      if (!validatedSchema.contains(key)) {
        failedTargets.add(key);
      }
    }
    log.info(
        String.format(
            "[StructuredLogValidator] log structure validation failed for %s", failedTargets));
    throw new BaseException(
        ExceptionCode.LOG_FORMAT_NOT_MATCHED,
        String.format(
            "[StructuredLogValidator] log structure validation failed for %s", failedTargets));
  }

  protected void validateJsonSchema(String logEventMsg) throws Exception {
    JsonNode logEventNode = mapper.readTree(logEventMsg);
    String key = getJsonSchemaMappingKey(logEventNode);
    if (validatedSchema.contains(key)) {
      return;
    }
    JsonSchema jsonSchema = schemasToValidate.get(key);
    if (jsonSchema != null) {
      ProcessingReport report = jsonSchema.validate(JsonLoader.fromString(logEventNode.toString()));
      if (report.isSuccess()) {
        validatedSchema.add(key);
      } else {
        // This will probably generate a lot of extra logs
        // may want to log this to a different level in the future.
        log.info("[StructuredLogValidator] failed to validate schema \n");
        log.info(report.toString() + "\n");
      }
    }
  }

  protected static JsonSchema parseJsonSchema(String templateInput) throws Exception {
    return parseJsonSchema(templateInput, LogLevel.FATAL);
  }

  protected static JsonSchema parseJsonSchema(String templateInput, LogLevel exceptionLevel)
      throws Exception {
    JsonNode jsonNode = JsonLoader.fromString(templateInput);
    JsonSchemaFactory jsonSchemaFactory =
        JsonSchemaFactory.newBuilder()
            .setReportProvider(new ListReportProvider(LogLevel.INFO, exceptionLevel))
            .freeze();
    return jsonSchemaFactory.getJsonSchema(jsonNode);
  }
}
