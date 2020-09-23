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
package com.amazon.aocagent.commands;

import com.amazon.aocagent.enums.TestAMI;
import com.amazon.aocagent.fileconfigs.ExpectedMetric;
import com.amazon.aocagent.fileconfigs.ExpectedTrace;
import com.amazon.aocagent.fileconfigs.OTConfig;
import com.amazon.aocagent.enums.TestCase;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.tasks.IntegTestFactory;
import lombok.SneakyThrows;
import picocli.CommandLine;

@CommandLine.Command(
    name = "integ-test",
    mixinStandardHelpOptions = true,
    description = "used for the integtests of the aocagent")
public class IntegTest implements Runnable {
  @CommandLine.Mixin CommonOption commonOption = new CommonOption();

  @CommandLine.Option(
      names = {"-a", "--ami"},
      description = "Enum values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}",
      defaultValue = "AMAZON_LINUX")
  private TestAMI testAMI;

  @CommandLine.Option(
      names = {"-c", "--config"},
      description = "Enum values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}",
      defaultValue = "DEFAULT_OT_CONFIG")
  private OTConfig otConfig;

  @CommandLine.Option(
      names = {"--expected-metric"},
      description = "Enum values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}",
      defaultValue = "DEFAULT_EXPECTED_METRIC")
  private ExpectedMetric expectedMetric;

  @CommandLine.Option(
      names = {"--expected-trace"},
      description = "Enum values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}",
      defaultValue = "DEFAULT_EXPECTED_TRACE")
  private ExpectedTrace expectedTrace;

  @CommandLine.Option(
      names = {"-t", "--test-case"},
      description = "EC2_TEST,ECS_TEST,EKS_TEST",
      defaultValue = "EC2_TEST")
  private TestCase testCase;

  @SneakyThrows
  @Override
  public void run() {
    Context context = commonOption.buildContext();
    context.setTestingAMI(testAMI.getTestAMIObj());
    context.setOtConfig(otConfig);
    context.setExpectedMetric(expectedMetric);
    context.setExpectedTrace(expectedTrace);
    IntegTestFactory.runTestCase(testCase, context);
  }
}
