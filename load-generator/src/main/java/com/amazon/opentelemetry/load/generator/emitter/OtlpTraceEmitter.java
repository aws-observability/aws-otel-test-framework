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
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.aws.trace.AwsXrayIdGenerator;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

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

    Resource resource = Resource.getDefault().merge(
            Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "load-generator")));

    OpenTelemetrySdk.builder()
            .setTracerProvider(
            SdkTracerProvider.builder()
                    .addSpanProcessor(
                            BatchSpanProcessor.builder(OtlpGrpcSpanExporter.getDefault())
                                    .setMaxQueueSize(5000)
                                    .setScheduleDelay(Duration.ofMillis(FLUSH_INTERVAL)).build())
                    .setIdGenerator(AwsXrayIdGenerator.getInstance())
                    .setResource(resource)
                    .build())
            .buildAndRegisterGlobal();

    tracer =
            GlobalOpenTelemetry.tracerBuilder("adot-load-generator-trace")
                    .setInstrumentationVersion("semver:0.1.0")
                    .build();
  }

  @Override
  public void nextDataPoint() {
    Span exampleSpan = tracer.spanBuilder("Example Span").setSpanKind(SpanKind.SERVER).startSpan();
    exampleSpan.setAttribute("good", "true");
    exampleSpan.setAttribute("exampleNumber", UUID.randomUUID().toString());
    exampleSpan.end();
  }
}
