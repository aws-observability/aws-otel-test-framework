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

import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.fileconfigs.LocalPathExpectedTemplate;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates ECS Prometheus structured logs.
 *
 * @see ContainerInsightPrometheusMetricsValidator for ECS Proemtheus Metrics
 * @see ContainerInsightPrometheusStructuredLogValidator for EKS
 */
@Log4j2
public class ContainerIInsightECSPrometheusStructuredLogValidator
    extends AbstractStructuredLogValidator {

  private List<CloudWatchContext.App> validateApps;

  @Override
  void init(Context context, FileConfig expectedDataTemplate) throws Exception {
    // /aws/ecs/containerinsights/aoc-prometheus-dashboard-1/prometheus
    logGroupName = String.format("/aws/ecs/containerinsights/%s/%s",
        context.getCloudWatchContext().getClusterName(), "prometheus");
    log.info("log group name is {}", logGroupName);

    // It's almost same as EKS prometheus but we use different key to find schema.
    validateApps = getAppsToValidate(context.getCloudWatchContext());
    MustacheHelper mustacheHelper = new MustacheHelper();

    for (CloudWatchContext.App app : validateApps) {
      FileConfig fileConfig = new LocalPathExpectedTemplate(FilenameUtils.concat(
          expectedDataTemplate.getPath().toString(),
          app.getName() + ".json"));
      String templateInput = mustacheHelper.render(fileConfig, context);
      // NOTE: EKS use namespace, we use task family for matching log event to schema.
      for (String taskDefinitionFamily : app.getTaskDefinitionFamilies()) {
        // We can deploy one workload in different ways (EC2, fargate etc.)
        // so we have a list of task definition families.
        schemasToValidate.put(taskDefinitionFamily, parseJsonSchema(templateInput));
      }
      logStreamNames.add(app.getJob());
    }
    log.info("apps to validate {} schema to validate {}", validateApps.size(),
        schemasToValidate.keySet());
  }

  @Override
  String getJsonSchemaMappingKey(JsonNode logEventNode) {
    // We use TaskDefinitionFamily to check because ServiceName is optional in EMF log.
    return logEventNode.get("TaskDefinitionFamily").asText();
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
