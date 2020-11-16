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

package com.amazon.opentelemetry.load.generator.command;

import com.amazon.opentelemetry.load.generator.emitter.Emitter;
import com.amazon.opentelemetry.load.generator.emitter.EmitterFactory;
import com.amazon.opentelemetry.load.generator.model.DataType;
import com.amazon.opentelemetry.load.generator.model.Parameter;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(
    name = "metric",
    mixinStandardHelpOptions = true,
    description = "enable metric data generator")
@Log4j2
public class MetricCommand implements Runnable {

  @Mixin
  CommonOption commonOption = new CommonOption();

  @Option(names = {"-f", "--flushInterval"},
      description = "the default metric collection interval",
      defaultValue = "5000")
  private long flushIntervalMillis = TimeUnit.SECONDS.toMillis(5);

  @SneakyThrows
  @Override
  public void run() {
    Parameter param = commonOption.buildParameter();
    param.setFlushInterval(flushIntervalMillis);
    log.info("param: {} " + param);

    Emitter emitter = EmitterFactory.getEmitter(param, DataType.Metric);

    emitter.emitDataLoad();

  }
}
