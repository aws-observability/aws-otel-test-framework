package com.amazon.aoc.validators;

import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.Context;
import com.amazonaws.services.logs.model.FilteredLogEvent;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



@Log4j2
public class ContainerInsightStructuredLogValidator
        extends AbstractStructuredLogValidator {

  private static final List<String> LOG_TYPE_TO_VALIDATE = Arrays.asList(
      "Cluster", 
      "ClusterNamespace",
      "ClusterService",
      "Container",
      "ContainerFS",
      "Node",
      "NodeDiskIO",
      "NodeFS",
      "NodeNet",
      "Pod",
      "PodNet"
  );

  private static final int MAX_RETRY_COUNT = 15;
  private static final int QUERY_LIMIT = 100;

  public ContainerInsightStructuredLogValidator() {
    super("performance");
  }

  private Map<String, JsonSchema> validateJsonSchema = new HashMap<>();


  @Override
  void init(Context context, String templatePath) throws Exception {
    MustacheHelper mustacheHelper = new MustacheHelper();
    for (String logType : LOG_TYPE_TO_VALIDATE) {
      String templateInput = mustacheHelper.render(new JsonSchemaFileConfig(
              FilenameUtils.concat(templatePath, logType + ".json")), context);
      validateJsonSchema.put(logType, parseJsonSchema(templateInput));
    }
  }

  @Override
  protected int getMaxRetryCount() {
    return MAX_RETRY_COUNT; 
  }

  private Set<String> getValidatingLogTypes() {
    Set<String> logTypes = new HashSet<>();
    for (String type : validateJsonSchema.keySet()) {
      logTypes.add(type);
    }
    return logTypes;
  }

  @Override
  protected void fetchAndValidateLogs(Instant startTime) throws Exception {
    Set<String> logTypes = getValidatingLogTypes();
    log.info("Fetch and validate logs with types: " + String.join(", ", logTypes));
    for (String logType : logTypes) {
      String filterPattern = String.format("{ $.Type = \"%s\"}", logType);
      List<FilteredLogEvent> logEvents = cloudWatchService.filterLogs(logGroupName, filterPattern, 
              startTime.toEpochMilli(), QUERY_LIMIT);
      for (FilteredLogEvent logEvent : logEvents) {
        validateJsonSchema(logEvent.getMessage());
      }
    } 
  }

  @Override
  JsonSchema findJsonSchemaForValidation(JsonNode logEventNode) {
    return validateJsonSchema.get(logEventNode.get("Type").asText());
  }

  @Override
  void updateJsonSchemaValidationResult(JsonNode logEventNode, boolean success) {
    if (success) {
      validateJsonSchema.remove(logEventNode.get("Type").asText());
    }
  }

  @Override
  void checkResult() throws Exception {
    if (validateJsonSchema.size() == 0) {
      return;
    }
    String[] failedTargets = new String[validateJsonSchema.size()];
    int i = 0;
    for (String logType : validateJsonSchema.keySet()) {
      failedTargets[i] = logType;
      i++;
    }
    throw new BaseException(
            ExceptionCode.LOG_FORMAT_NOT_MATCHED,
            String.format("[ContainerInsight] log structure validation failed for Type = %s",
                    StringUtils.join(",", failedTargets)));
  }

}