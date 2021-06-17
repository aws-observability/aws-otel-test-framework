package com.amazon.aoc.validators;


import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.fileconfigs.LocalPathExpectedTemplate;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates ECS Prometheus structured logs.
 *
 * @see ContainerInsightPrometheusMetricsValidator for ECS Proemtheus Metrics
 * @see ContainerInsightPrometheusStructuredLogValidator for EKS
 */
public class ContainerIInsightECSPrometheusStructuredLogValidator
    extends AbstractStructuredLogValidator {

  private List<CloudWatchContext.App> validateApps;

  @Override
  void init(Context context, FileConfig expectedDataTemplate) throws Exception {
    // /aws/ecs/containerinsights/aoc-prometheus-dashboard-1/prometheus
    logGroupName = String.format("/aws/ecs/containerinsights/%s/%s",
        context.getCloudWatchContext().getClusterName(), "prometheus");

    // TODO: it's same as eks prometheus
    validateApps = getAppsToValidate(context.getCloudWatchContext());
    MustacheHelper mustacheHelper = new MustacheHelper();

    for (CloudWatchContext.App app : validateApps) {
      FileConfig fileConfig = new LocalPathExpectedTemplate(FilenameUtils.concat(
          expectedDataTemplate.getPath().toString(),
          app.getName() + ".json"));
      String templateInput = mustacheHelper.render(fileConfig, context);
      schemasToValidate.put(app.getNamespace(), parseJsonSchema(templateInput));
      logStreamNames.add(app.getJob());
    }
  }

  @Override
  String getJsonSchemaMappingKey(JsonNode logEventNode) {
    // We use TaskDefinitionFamily to check because ServiceName is optional in EMF log.
    String taskFamily = logEventNode.get("TaskDefinitionFamily").asText();
    // When registering schema in schemasToValidate we
    if (taskFamily.contains("jmx")) {
      return "jmx";
    }
    if (taskFamily.contains("nginx")) {
      return "nginx";
    }
    return null;
  }

  private static List<CloudWatchContext.App> getAppsToValidate(CloudWatchContext cwContext) {
    List<CloudWatchContext.App> apps = new ArrayList<>();
    if (cwContext.getNginx() != null) {
      apps.add(cwContext.getNginx());
    }
    if (cwContext.getJmx() != null) {
      apps.add(cwContext.getJmx());
    }
    return apps;
  }
}
