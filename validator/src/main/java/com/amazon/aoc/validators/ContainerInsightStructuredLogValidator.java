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
import com.amazon.aoc.helpers.MustacheHelper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class ContainerInsightStructuredLogValidator implements IValidator {
  private CloudWatchService cloudWatchService;
  private final ObjectMapper mapper = new ObjectMapper();

  private String logGroupName;

  private Map<String, JsonSchema> jsonSchemaForApps;
  private List<CloudWatchContext.App> validateApps;

  private static final int MAX_RETRY_COUNT = 6;
  private static final int QUERY_LIMIT = 500;
  private static final int CHECK_INTERVAL_IN_MILLI = 30 * 1000;
  private static final int CHECK_DURATION_IN_SECONDS = 2 * 60;

  @Override
  public void init(Context context, ValidationConfig validationConfig, ICaller caller,
                   FileConfig expectedDataTemplate) throws Exception {
    cloudWatchService = new CloudWatchService(context.getRegion());

    CloudWatchContext cwContext = context.getCloudWatchContext();
    logGroupName = String.format("/aws/containerinsights/%s/prometheus",
            cwContext.getClusterName());
    validateApps = getAppsToValidate(cwContext);

    MustacheHelper mustacheHelper = new MustacheHelper();
    String templateInput = mustacheHelper.render(expectedDataTemplate, context);
    jsonSchemaForApps = parseJsonSchemaMap(templateInput);
  }

  @Override
  public void validate() throws Exception {
    log.info("[ContainerInsight] start validating structured log");

    RetryHelper.retry(MAX_RETRY_COUNT, CHECK_INTERVAL_IN_MILLI, true, () -> {
      Instant startTime = Instant.now().minusSeconds(CHECK_DURATION_IN_SECONDS)
              .truncatedTo(ChronoUnit.MINUTES);

      for (CloudWatchContext.App validatingApp : validateApps) {
        List<OutputLogEvent> logEvents = cloudWatchService.getLogs(logGroupName,
                validatingApp.getJob(), startTime.getLong(ChronoField.MILLI_OF_SECOND),
                QUERY_LIMIT);
        validateLogEvents(validatingApp, logEvents);
      }
    });
    log.info("[ContainerInsight] finish validation successfully");
  }

  private void validateLogEvents(CloudWatchContext.App validatingApp,
                                 List<OutputLogEvent> logEvents) throws Exception {
    JsonSchema appSchema = jsonSchemaForApps.get(validatingApp.getName());

    for (OutputLogEvent logEvent : logEvents) {
      JsonNode logEventNode = mapper.readTree(logEvent.getMessage());
      JsonNode logEventNamespaceNode = logEventNode.get("Namespace");
      String logEventNamespace = logEventNamespaceNode == null ? "" :
              logEventNamespaceNode.textValue();

      if (!validatingApp.getNamespace().equals(logEventNamespace)) {
        log.debug(String.format("[ContainerInsight] skip log validation: namespace not matched. "
                + "Expected: %s, actual: %s", validatingApp.getNamespace(), logEventNamespace));
      }

      ProcessingReport report = appSchema.validate(JsonLoader.fromString(logEventNode.toString()));
      if (report.isSuccess()) {
        return;
      }
    }
    throw new BaseException(
            ExceptionCode.LOG_FORMAT_NOT_MATCHED, String.format("[ContainerInsight] none of the "
            + "collected logs matches %s log structure", validatingApp.getName()));
  }

  private static List<CloudWatchContext.App> getAppsToValidate(CloudWatchContext cwContext) {
    List<CloudWatchContext.App> apps = new ArrayList<>();
    if (cwContext.getAppMesh() != null) {
      apps.add(cwContext.getAppMesh());
    }
    if (cwContext.getNginx() != null) {
      apps.add(cwContext.getNginx());
    }
    if (cwContext.getHaproxy() != null) {
      apps.add(cwContext.getHaproxy());
    }
    if (cwContext.getJmx() != null) {
      apps.add(cwContext.getJmx());
    }
    if (cwContext.getMemcached() != null) {
      apps.add(cwContext.getMemcached());
    }
    return apps;
  }

  private static Map<String, JsonSchema> parseJsonSchemaMap(String templateInput) throws Exception {
    Map<String, JsonSchema> jsonSchemaMap = new HashMap<>();
    JsonNode schemaArrayNode = JsonLoader.fromString(templateInput);
    JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
    for (JsonNode schemaNode : schemaArrayNode) {
      if (schemaNode.get("app") == null || schemaNode.get("schema") == null) {
        throw new BaseException(ExceptionCode.DATA_MODEL_NOT_MATCHED,
                "Invalid schema format, missing property app or schema");
      }
      String appName = schemaNode.get("app").asText();
      JsonSchema appSchema = jsonSchemaFactory.getJsonSchema(schemaNode.get("schema"));
      jsonSchemaMap.put(appName, appSchema);
    }
    return jsonSchemaMap;
  }

}
