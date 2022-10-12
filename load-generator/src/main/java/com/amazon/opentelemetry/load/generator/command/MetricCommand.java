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
      description = "the metric collection export interval (in ms)",
      defaultValue = "1000")
  private long flushIntervalMillis = TimeUnit.SECONDS.toMillis(1);

  @Option(names = {"-m", "--metricCount"},
          description = "the number of metrics that should be created for each metric type",
          defaultValue = "1")
  private int metricCount;

  @Option(names = {"-dp", "--datapointCount"},
          description = "the number of datapoints that should be created for each metric",
          defaultValue = "1")
  private int datapointCount;

  @Option(names = {"-o", "--observationInterval"},
          description = "the interval (in ms) at which metric observations/values are updated",
          defaultValue = "1000")
  private long observationIntervalMillis = TimeUnit.SECONDS.toMillis(1);

  @Option(names = {"-mt", "--metricType"},
          description = "Specify the type of metric - counter or gauge",
          defaultValue = "counter")
  private String metricType;

  @SneakyThrows
  @Override
  public void run() {
    Parameter param = commonOption.buildParameter();
    param.setFlushInterval(flushIntervalMillis);
    param.setMetricCount(metricCount);
    param.setDatapointCount(datapointCount);
    param.setObservationInterval(observationIntervalMillis);
    param.setMetricType(metricType);


    log.info("param: {} " + param);

    Emitter emitter = EmitterFactory.getEmitter(param, DataType.Metric);

    emitter.emitDataLoad();

  }
}
