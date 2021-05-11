package com.amazon.aoc.validators;

import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.fileconfigs.LocalPathExpectedTemplate;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.Context;
import com.amazonaws.services.logs.model.FilteredLogEvent;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Log4j2
public class ConatinerInsightECSStructuredLogValidator
        extends AbstractStructuredLogValidator {

  private static final List<String> LOG_TYPE_TO_VALIDATE = Arrays.asList(
          "Instance",
          "InstanceDiskIO",
          "InstanceFS",
          "InstanceNet"
  );

  private static final int MAX_RETRY_COUNT = 15;
  private static final int QUERY_LIMIT = 100;
  private static final String LOGGROUPPATH = "/aws/ecs/containerinsights/%s/performance";
  private static final String FORMATPATTERN = "{ $.Type = \"%s\"}";
  private static final String TYPE = "Type";

  @Override
  void init(Context context, FileConfig expectedDataTemplate) throws Exception {
    logGroupName = String.format(LOGGROUPPATH,
            context.getCloudWatchContext().getClusterName());
    MustacheHelper mustacheHelper = new MustacheHelper();
    for (String logType : LOG_TYPE_TO_VALIDATE) {
      FileConfig fileConfig = new LocalPathExpectedTemplate(FilenameUtils.concat(
              expectedDataTemplate.getPath().toString(),
              logType + ".json"));
      String templateInput = mustacheHelper.render(fileConfig, context);
      schemasToValidate.put(logType, parseJsonSchema(templateInput));
    }
  }

  @Override
  String getJsonSchemaMappingKey(JsonNode jsonNode) {
    return jsonNode.get(TYPE).asText();
  }

  @Override
  protected int getMaxRetryCount() {
    return MAX_RETRY_COUNT;
  }

  @Override
  protected void fetchAndValidateLogs(Instant startTime) throws Exception {
    Set<String> logTypes = new HashSet<>(LOG_TYPE_TO_VALIDATE);
    log.info("Fetch and validate logs with types: " + String.join(", ", logTypes));
    for (String logType : logTypes) {
      String filterPattern = String.format(FORMATPATTERN, logType);
      List<FilteredLogEvent> logEvents = cloudWatchService.filterLogs(logGroupName, filterPattern,
              startTime.toEpochMilli(), QUERY_LIMIT);
      for (FilteredLogEvent logEvent : logEvents) {
        validateJsonSchema(logEvent.getMessage());
      }
    }
  }

}