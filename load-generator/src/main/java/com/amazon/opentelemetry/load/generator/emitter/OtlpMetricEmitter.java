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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import java.util.concurrent.ThreadLocalRandom;
import java.time.Duration;

public class OtlpMetricEmitter extends MetricEmitter {

  private final static String API_NAME = "loadTest";
  private final static Attributes METRIC_ATTRIBUTES = Attributes.of(AttributeKey.stringKey(DIMENSION_API_NAME), API_NAME,
          AttributeKey.stringKey(DIMENSION_STATUS_CODE), "200");
  LongCounter apiBytesSentCounter;
  Long apiLatency;

  public OtlpMetricEmitter(Parameter param) {
    super();
    this.param = param;
  }

  @Override
  public void emitDataLoad() throws Exception {
    this.setupProvider();
    this.start(() -> nextDataPoint());
  }

  @Override
  public void nextDataPoint() {
    apiBytesSentCounter
            .add(10, METRIC_ATTRIBUTES);
    mutateApiLatency();
  }

  @Override
  public void setupProvider() {
    Resource resource = Resource.getDefault().merge(
            Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "load-generator")));

    OpenTelemetrySdk.builder().setMeterProvider(
                    SdkMeterProvider.builder()
                            .registerMetricReader(
                                    PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().setEndpoint("http://" + param.getEndpoint()).build())
                                            .setInterval(Duration.ofMillis(param.getFlushInterval())).build())
                            .setResource(resource)

                            .build())
            .buildAndRegisterGlobal();

    Meter meter =
            GlobalOpenTelemetry.meterBuilder("aws-otel-load-generator-metric")
                    .setInstrumentationVersion("0.1.0")
                    .build();

    apiBytesSentCounter =
            meter.counterBuilder(API_COUNTER_METRIC)
                    .setDescription("API request load sent in bytes")
                    .setUnit("one")
                    .build();

    meter.gaugeBuilder(API_LATENCY_METRIC)
            .setDescription("API latency time")
            .setUnit("ms")
            .ofLongs()
            .buildWithCallback(measurement -> measurement.record(
                    apiLatency, METRIC_ATTRIBUTES));
  }

  private void mutateApiLatency() {
    this.apiLatency = ThreadLocalRandom.current().nextLong(-100,100);
  }
}