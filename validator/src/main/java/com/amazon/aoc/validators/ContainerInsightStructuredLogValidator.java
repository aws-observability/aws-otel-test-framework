package com.amazon.aoc.validators;

import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.fileconfigs.LocalPathExpectedTemplate;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.Context;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.logs.model.FilteredLogEvent;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;


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

  @Override
  void init(Context context, FileConfig expectedDataTemplate) throws Exception {
    logGroupName = String.format("/aws/containerinsights/%s/performance",
            context.getCloudWatchContext().getClusterName());
    MustacheHelper mustacheHelper = new MustacheHelper();
    for (String logType : LOG_TYPE_TO_VALIDATE) {
      FileConfig fileConfig = new LocalPathExpectedTemplate(FilenameUtils.concat(
          expectedDataTemplate.getPath().toString(),
          logType + ".json"));
      try {
        String templateInput = mustacheHelper.render(fileConfig, context);
        schemasToValidate.put(logType, parseJsonSchema(templateInput));
      } catch (IOException e) {
        log.debug("The " + logType + " was not found for this expected template path.");
      }
    }
  }

  @Override
  String getJsonSchemaMappingKey(JsonNode jsonNode) {
    return jsonNode.get("Type").asText();
  }

  @Override
  protected int getMaxRetryCount() {
    return MAX_RETRY_COUNT;
  }

  @Override
  protected void fetchAndValidateLogs(Instant startTime) throws Exception {
    log.info("Fetch and validate logs with types: "
            + String.join(", ", schemasToValidate.keySet()));

    for (String logType : schemasToValidate.keySet()) {
      String filterPattern = String.format("{ $.Type = \"%s\"}", logType);
      try {
        log.info(String.format("[StructuredLogValidator] Filtering logs in log group %s"
                + "with filter pattern %s", logGroupName, filterPattern));
        List<FilteredLogEvent> logEvents = cloudWatchService.filterLogs(logGroupName, filterPattern,
                startTime.toEpochMilli(), QUERY_LIMIT);
        for (FilteredLogEvent logEvent : logEvents) {
          validateJsonSchema(logEvent.getMessage());
        }
      } catch (AmazonClientException e) {
        log.info(String.format("[StructuredLogValidator] failed to retrieve filtered logs "
                + "in log group %s with filter pattern %s", logGroupName, filterPattern));
        throw e;
      }
    }
  }

}