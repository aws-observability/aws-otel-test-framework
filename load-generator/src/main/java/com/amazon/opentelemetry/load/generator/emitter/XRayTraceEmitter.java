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
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import java.util.UUID;

public class XRayTraceEmitter extends TraceEmitter {

  public XRayTraceEmitter(Parameter param) {
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
  }

  @Override
  public void nextDataPoint() {
    System.out.println("segment started");
    Segment segment = AWSXRay.beginSegment("service");
    System.out.println("subsegment started");
    Subsegment subsegment = AWSXRay.beginSubsegment("## SessionModel.saveSession");
    System.out.println("subsegment created");
    subsegment.addException(new Exception("test"));
    segment.addSubsegment(subsegment);
    AWSXRay.endSubsegment();
    AWSXRay.endSegment();
    System.out.println("segment created");

  }
}
