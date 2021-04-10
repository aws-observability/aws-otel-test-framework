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
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.logs.model.OutputLogEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Log4j2
public abstract class AbstractStructuredLogValidator implements IValidator {
  private CloudWatchService cloudWatchService;
  private final ObjectMapper mapper = new ObjectMapper();

  private final String validateType;

  private String logGroupName;

  private static final int MAX_RETRY_COUNT = 6;
  private static final int QUERY_LIMIT = 500;
  private static final int CHECK_INTERVAL_IN_MILLI = 30 * 1000;
  private static final int CHECK_DURATION_IN_SECONDS = 2 * 60;

  public AbstractStructuredLogValidator(String validateType) {
    this.validateType = validateType;
  }

  @Override
  public void init(Context context, ValidationConfig validationConfig, ICaller caller,
                   FileConfig expectedDataTemplate) throws Exception {
    cloudWatchService = new CloudWatchService(context.getRegion());

    CloudWatchContext cwContext = context.getCloudWatchContext();
    logGroupName = String.format("/aws/containerinsights/%s/%s",
            cwContext.getClusterName(), validateType);
    init(context, expectedDataTemplate.getPath());
  }

  abstract void init(Context context, String templatePath) throws Exception;

  abstract Set<String> getValidatingLogStreamNames();

  abstract JsonSchema findJsonSchemaForValidation(JsonNode logEventNode);

  abstract void updateJsonSchemaValidationResult(JsonNode logEventNode, boolean success);

  abstract void checkResult() throws Exception;

  @Override
  public void validate() throws Exception {
    log.info("[ContainerInsight] start validating structured log of " + validateType);

    RetryHelper.retry(MAX_RETRY_COUNT, CHECK_INTERVAL_IN_MILLI, true, () -> {
      Instant startTime = Instant.now().minusSeconds(CHECK_DURATION_IN_SECONDS)
              .truncatedTo(ChronoUnit.MINUTES);

      for (String logStreamName : getValidatingLogStreamNames()) {
        List<OutputLogEvent> logEvents = cloudWatchService.getLogs(logGroupName, logStreamName,
                startTime.getLong(ChronoField.MILLI_OF_SECOND), QUERY_LIMIT);
        if (logEvents.isEmpty()) {
          throw new BaseException(
                  ExceptionCode.LOG_FORMAT_NOT_MATCHED,
                  String.format("[ContainerInsight] no logs found under log stream %s",
                          logStreamName));
        }
        for (OutputLogEvent logEvent : logEvents) {
          JsonNode logEventNode = mapper.readTree(logEvent.getMessage());
          JsonSchema jsonSchema = findJsonSchemaForValidation(logEventNode);
          if (jsonSchema != null) {
            ProcessingReport report = jsonSchema.validate(
                    JsonLoader.fromString(logEventNode.toString()));
            updateJsonSchemaValidationResult(logEventNode, report.isSuccess());
          }
        }
      }
      checkResult();
    });
    log.info("[ContainerInsight] finish validation successfully");
  }


  protected static JsonSchema parseJsonSchema(String templateInput) throws Exception {
    JsonNode jsonNode = JsonLoader.fromString(templateInput);
    JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
    return jsonSchemaFactory.getJsonSchema(jsonNode);
  }

  static class JsonSchemaFileConfig implements FileConfig {
    private String path;

    public JsonSchemaFileConfig(String path) {
      this.path = path;
    }

    @Override
    public String getPath() {
      return path;
    }
  }
}
