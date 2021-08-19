/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amazon.opentelemetry.trace.client.JaegerTraceEmitClient;
import com.amazon.opentelemetry.trace.client.TraceClient;

public class JaegerTraceEmitter extends TraceEmitter {

  private TraceClient client;

  public JaegerTraceEmitter(Parameter param) {
    super();
    this.param = param;
  }

  @Override
  public void emitDataLoad() throws Exception {
    this.setupProvider();
    this.start(() -> nextDataPoint());
  }

  @Override
  public void setupProvider() throws Exception {
    this.client = new JaegerTraceEmitClient(this.param.getEndpoint());
  }

  @Override
  public void nextDataPoint() {
    try {
      client.emit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
