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

import com.amazon.opentelemetry.load.generator.model.DataType;
import com.amazon.opentelemetry.load.generator.model.Parameter;
import com.amazon.opentelemetry.load.generator.util.Constants;
import com.google.common.base.Preconditions;

public class EmitterFactory {

  public static Emitter getEmitter(Parameter param, DataType dataType) {
    Preconditions.checkArgument(param.getDataFormat() != null, "data format value is null");
    switch (dataType) {
      case Metric:
        return getMetricEmitter(param);
      case Trace:
        return getTraceEmitter(param);
//      case Log:
//        return getLogEmitter(param);
      default:
        throw new RuntimeException("invalid data type specified");
    }
  }

  private static Emitter getMetricEmitter(Parameter param) {
    if (param.getDataFormat().equalsIgnoreCase(Constants.OTLP)) {
      return new OtlpMetricEmitter(param);
    } else {
      throw new RuntimeException("unknown metric data format specified");
    }
  }

  private static Emitter getTraceEmitter(Parameter param) {
    if (param.getDataFormat().equalsIgnoreCase(Constants.OTLP)) {
      return new OtlpTraceEmitter(param);
    } else if (param.getDataFormat().equalsIgnoreCase(Constants.XRAY)) {
      return new XRayTraceEmitter(param);
    } else {
      throw new RuntimeException("unknown trace data format specified");
    }
  }

//  private static Emitter getLogEmitter(Parameter param) {
//    return new
//  }

}
