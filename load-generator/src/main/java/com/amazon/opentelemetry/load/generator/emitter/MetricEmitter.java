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

package com.amazon.opentelemetry.load.generator.emitter;

import com.amazon.opentelemetry.load.generator.model.Parameter;
import java.util.concurrent.TimeUnit;

public abstract class MetricEmitter implements Emitter {

  protected static final String DIMENSION_API_NAME = "apiName";
  protected static final String DIMENSION_STATUS_CODE = "statusCode";
  protected static String API_COUNTER_METRIC = "apiBytesSent";
  protected static String API_LATENCY_METRIC = "latency";

  protected Parameter param;

  @Override
  public void start(Runnable emitter) {
    scheduler.scheduleAtFixedRate(emitter, TimeUnit.SECONDS.toMillis(5000),
        TimeUnit.SECONDS.toNanos(1) / this.param.getRate(), TimeUnit.NANOSECONDS);
  };

}
