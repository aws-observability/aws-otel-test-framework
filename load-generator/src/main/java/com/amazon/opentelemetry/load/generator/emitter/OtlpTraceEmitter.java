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
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.extension.aws.trace.AwsXrayIdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Duration;
import java.util.UUID;

public class OtlpTraceEmitter extends TraceEmitter {

  Tracer tracer;

  public OtlpTraceEmitter(Parameter param) {
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
    OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint(param.getEndpoint())
        .setTimeout(Duration.ofMillis(10))
        .build();

    SdkTracerProviderBuilder builder = SdkTracerProvider.builder();
    builder.setIdGenerator(AwsXrayIdGenerator.getInstance());
    builder.addSpanProcessor(SimpleSpanProcessor.create(spanExporter));
    TracerProvider tracerProvider = builder.build();

    tracer =
        tracerProvider.get("aws-otel-load-generator-trace", "semver:0.1.0");

  }

  @Override
  public void nextDataPoint() {
    Span exampleSpan = tracer.spanBuilder("Example Span").setSpanKind(SpanKind.SERVER).startSpan();

    exampleSpan.setAttribute("good", "true");
    exampleSpan.setAttribute("exampleNumber", UUID.randomUUID().toString());
    exampleSpan.end();
  }
}
