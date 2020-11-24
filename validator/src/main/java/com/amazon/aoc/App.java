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
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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

  @CommandLine.Option(
      names = {"--mocked-server-validating-url"},
      description = "mocked server validating url")
  private String mockedServerValidatingUrl;

  @CommandLine.Option(
          names = {"--canary"},
          defaultValue = "false")
  private boolean isCanary;

  public static void main(String[] args) throws Exception {
    int exitCode = new CommandLine(new App()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    final Instant startTime = Instant.now();
    // build context
    Context context = new Context(this.testingId, this.region, this.isCanary);
    context.setMetricNamespace(this.metricNamespace);
    context.setEndpoint(this.endpoint);
    context.setEcsContext(buildECSContext(ecsContexts));
    context.setAlarmNameList(alarmNameList);
    context.setMockedServerValidatingUrl(mockedServerValidatingUrl);

    log.info(context);

    final AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();

    // load config
    List<ValidationConfig> validationConfigList =
        new ConfigLoadHelper().loadConfigFromFile(configPath);

    // run validation
    int maxValidationCycles = 1;
    ValidatorFactory validatorFactory = new ValidatorFactory(context);
    if (this.isCanary) {
      maxValidationCycles = 30;
    }
    for (int cycle = 0; cycle < maxValidationCycles; cycle++) {
      for (ValidationConfig validationConfigItem : validationConfigList) {
        try {
          validatorFactory.launchValidator(validationConfigItem).validate();
        } catch (Exception e) {
          if (this.isCanary) {
            //emit metric
            MetricDatum datum = new MetricDatum()
                    .withMetricName("Success")
                    .withUnit(StandardUnit.None)
                    .withValue(0.0);
            PutMetricDataRequest request = new PutMetricDataRequest()
                    .withNamespace("Otel/Canary")
                    .withMetricData(datum);
            cw.putMetricData(request);
          }
          throw e;
        }
      }
      if (maxValidationCycles - cycle - 1 > 0) {
        log.info("Completed {} validation cycle for current canary test. "
                        + "Still need to validate {} cycles. Sleep 1 minute then proceed.",
                cycle + 1, maxValidationCycles - cycle - 1);
        TimeUnit.MINUTES.sleep(1);
      }
    }
    Instant endTime = Instant.now();
    Duration duration = Duration.between(startTime, endTime);
    log.info("Validation has completed in {} minutes.", duration.toMinutes());
    if (this.isCanary) {
      //emit metric
      MetricDatum datum = new MetricDatum()
              .withMetricName("Success")
              .withUnit(StandardUnit.None)
              .withValue(1.0);
      PutMetricDataRequest request = new PutMetricDataRequest()
              .withNamespace("Otel/Canary")
              .withMetricData(datum);
      cw.putMetricData(request);
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
