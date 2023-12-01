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

import com.amazon.opentelemetry.load.generator.model.Parameter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(footer = "Common parameter options for both Metric and Trace")
public class CommonOption {

  @Option(names = {"-r", "--rate"},
          description = "the number of data points will be sent per second",
          defaultValue = "10")
  private int rate;
  @Option(names = {"-u", "--url"},
      description = "adot-collector receiver endpoint",
      defaultValue = "localhost:4317")
  private String endpoint;

  @Option(names = {"-d", "--dataFormat"},
      description = "the data format for metrics or traces. Eg, otlp, xray",
      defaultValue = "otlp")
  private String dataFormat;


  public Parameter buildParameter() {
    return Parameter.builder()
        .rate(rate)
        .dataFormat(dataFormat)
        .endpoint(endpoint)
        .build();
  }
}
