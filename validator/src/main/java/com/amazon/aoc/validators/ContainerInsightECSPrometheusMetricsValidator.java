package com.amazon.aoc.validators;

import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.fileconfigs.LocalPathExpectedTemplate;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.FilenameUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ContainerInsightECSPrometheusMetricsValidator extends AbstractCWMetricsValidator {
  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  // TODO: it's same as eks prometheus ...
  @Override
  List<Metric> getExpectedMetrics(
      Context context,
      FileConfig expectedDataTemplate
  ) throws Exception {
    List<Metric> expectedMetrics = new ArrayList<>();
    List<CloudWatchContext.App> validateApps = getAppsToValidate(context.getCloudWatchContext());
    MustacheHelper mustacheHelper = new MustacheHelper();
    for (CloudWatchContext.App app : validateApps) {
      FileConfig fileConfig = new LocalPathExpectedTemplate(FilenameUtils.concat(
          expectedDataTemplate.getPath().toString(),
          app.getName() + "_metrics.mustache"));
      String templateInput = mustacheHelper.render(fileConfig, context);
      List<Metric> appMetrics = mapper.readValue(templateInput.getBytes(StandardCharsets.UTF_8),
          new TypeReference<List<Metric>>() {
          });
      expectedMetrics.addAll(appMetrics);
    }
    return expectedMetrics;
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
