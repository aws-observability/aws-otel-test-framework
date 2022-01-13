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
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class OtlpMetricEmitter extends MetricEmitter {

  private final static String API_NAME = "loadTest";
  LongCounter apiBytesSentCounter;
  LongHistogram apiLatencyRecorder;

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
    apiLatencyRecorder.record(200, Attributes.builder().put(DIMENSION_API_NAME, API_NAME).put(DIMENSION_STATUS_CODE, "200").build());

    apiBytesSentCounter
        .add(100, Attributes.builder().put(DIMENSION_API_NAME, API_NAME).put(DIMENSION_STATUS_CODE, "200").build());
  }

  @Override
  public void setupProvider() {
    MetricExporter metricExporter =
        OtlpGrpcMetricExporter.builder()
                .setEndpoint(param.getEndpoint())
                .setTimeout(Duration.ofMillis(10))
                .build();

    SdkMeterProvider sdkMeterProvider = SdkMeterProvider
            .builder()
            .registerMetricReader(PeriodicMetricReader.builder(metricExporter).setInterval(param.getFlushInterval(), TimeUnit.NANOSECONDS).newMetricReaderFactory())
            .build();
    Meter meter = sdkMeterProvider.meterBuilder("aws-otel-load-generator-metric")
            .setInstrumentationVersion("0.1.0")
            .build();

    apiBytesSentCounter = meter
        .counterBuilder(API_COUNTER_METRIC)
        .setDescription("API request load sent in bytes")
        .setUnit("one")
        .build();

    apiLatencyRecorder =
        meter
            .histogramBuilder(API_LATENCY_METRIC)
            .ofLongs()
            .setDescription("API latency time")
            .setUnit("ms")
            .build();
  }

}
