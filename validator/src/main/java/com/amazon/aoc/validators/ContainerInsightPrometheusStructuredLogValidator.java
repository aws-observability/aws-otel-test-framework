package com.amazon.aoc.validators;

import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
public class ContainerInsightPrometheusStructuredLogValidator
        extends AbstractStructuredLogValidator {

  public ContainerInsightPrometheusStructuredLogValidator() {
    super("prometheus");
  }

  private Map<String, JsonSchema> validateJsonSchema = new HashMap<>();
  private List<CloudWatchContext.App> validateApps;


  @Override
  void init(Context context, String templatePath) throws Exception {
    validateApps = getAppsToValidate(context.getCloudWatchContext());
    MustacheHelper mustacheHelper = new MustacheHelper();

    for (CloudWatchContext.App app : validateApps) {
      String templateInput = mustacheHelper.render(new JsonSchemaFileConfig(
              FilenameUtils.concat(templatePath, app.getName() + ".json")), context);
      validateJsonSchema.put(app.getNamespace(), parseJsonSchema(templateInput));
    }
  }

  @Override
  Set<String> getValidatingLogStreamNames() {
    Set<String> logStreamNames = new HashSet<>();
    for (CloudWatchContext.App validateApp : validateApps) {
      logStreamNames.add(validateApp.getJob());
    }
    return logStreamNames;
  }

  @Override
  JsonSchema findJsonSchemaForValidation(JsonNode logEventNode) {
    return validateJsonSchema.get(logEventNode.get("Namespace").asText());
  }

  @Override
  void updateJsonSchemaValidationResult(JsonNode logEventNode, boolean success) {
    if (success) {
      validateJsonSchema.remove(logEventNode.get("Namespace").asText());
    }
  }

  @Override
  void checkResult() throws Exception {
    if (validateJsonSchema.size() == 0) {
      return;
    }
    String[] failedTargets = new String[validateJsonSchema.size()];
    int i = 0;
    for (String appNamespace : validateJsonSchema.keySet()) {
      failedTargets[i] = appNamespace;
      i++;
    }
    throw new BaseException(
            ExceptionCode.LOG_FORMAT_NOT_MATCHED,
            String.format("[ContainerInsight] log structure validation failed in namespace %s",
                    StringUtils.join(",", failedTargets)));
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

}