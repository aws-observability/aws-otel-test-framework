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

import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.tasks.TaskFactory;
import lombok.SneakyThrows;
import picocli.CommandLine;

@CommandLine.Command(
    name = "clean",
    mixinStandardHelpOptions = true,
    description = "use to clean resources of the aocintegtest")
public class ResourceClean implements Runnable {
  @CommandLine.Mixin CommonOption commonOption = new CommonOption();

  @CommandLine.Option(
      names = {"-t", "--clean-task"},
      description = "EC2Clean, ECSClean, EKSClean",
      defaultValue = "EC2Clean")
  private String cleanTask;

  @SneakyThrows
  @Override
  public void run() {
    Context context = commonOption.buildContext();
    TaskFactory.executeTask(cleanTask, context);
  }
}
