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

package com.amazon.aoc;

import com.amazon.aoc.helpers.ConfigLoadHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ECSContext;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.validators.ValidatorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "e2etest", mixinStandardHelpOptions = true, version = "1.0")
@Log4j2
public class App implements Callable<Integer> {

  @CommandLine.Option(names = {"-c", "--config-path"})
  private String configPath;

  @CommandLine.Option(
      names = {"-t", "--testing-id"},
      defaultValue = "dummy-id")
  private String testingId;

  @CommandLine.Option(
      names = {"--metric-namespace"},
      defaultValue = "AWSObservability/CloudWatchOTService")
  private String metricNamespace;

  @CommandLine.Option(names = {"--endpoint"})
  private String endpoint;

  @CommandLine.Option(
      names = {"--region"},
      defaultValue = "us-west-2")
  private String region;

  @CommandLine.Option(
      names = {"--ecs-context"},
      description = "eg, --ecs-context ecsCluster=xxx --ecs-context ecsTaskArn=xxxx")
  private Map<String, String> ecsContexts;

  @CommandLine.Option(
      names = {"--alarm-names"},
      description = "the cloudwatch alarm names")
  private List<String> alarmNameList;

  public static void main(String[] args) throws Exception {
    int exitCode = new CommandLine(new App()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    // build context
    Context context = new Context(this.testingId, this.region);
    context.setMetricNamespace(this.metricNamespace);
    context.setEndpoint(this.endpoint);
    context.setEcsContext(buildECSContext(ecsContexts));
    context.setAlarmNameList(alarmNameList);

    log.info(context);

    // load config
    List<ValidationConfig> validationConfigList =
        new ConfigLoadHelper().loadConfigFromFile(configPath);

    // run validation
    ValidatorFactory validatorFactory = new ValidatorFactory(context);
    for (ValidationConfig validationConfigItem : validationConfigList) {
      validatorFactory.launchValidator(validationConfigItem).validate();
    }
    return null;
  }

  private ECSContext buildECSContext(Map<String, String> ecsContextMap) {
    if (ecsContextMap == null) {
      return null;
    }
    return new ObjectMapper().convertValue(ecsContextMap, ECSContext.class);
  }
}
