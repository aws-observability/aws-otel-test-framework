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
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.common.Labels;
import io.opentelemetry.exporters.otlp.OtlpGrpcMetricExporter;
import io.opentelemetry.metrics.LongCounter;
import io.opentelemetry.metrics.LongValueRecorder;
import io.opentelemetry.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collections;

public class OtlpMetricEmitter extends MetricEmitter {

  private final static String API_NAME = "loadTest";
  LongCounter apiBytesSentCounter;
  LongValueRecorder apiLatencyRecorder;

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
    apiLatencyRecorder.record(200, Labels
        .of(DIMENSION_API_NAME, API_NAME, DIMENSION_STATUS_CODE, "200"));

    apiBytesSentCounter
        .add(100, Labels.of(DIMENSION_API_NAME, API_NAME, DIMENSION_STATUS_CODE, "200"));
  }

  @Override
  public void setupProvider() {
    MetricExporter metricExporter =
        OtlpGrpcMetricExporter.newBuilder()
            .setChannel(
                ManagedChannelBuilder.forTarget(param.getEndpoint()).usePlaintext().build())
            .build();

    IntervalMetricReader.builder()
        .setMetricProducers(
            Collections.singleton(OpenTelemetrySdk.getMeterProvider().getMetricProducer()))
        .setExportIntervalMillis(param.getFlushInterval())
        .setMetricExporter(metricExporter)
        .build();

    Meter meter = OpenTelemetry.getMeter("aws-otel-load-generator-metric", "0.1.0");

    apiBytesSentCounter = meter
        .longCounterBuilder(API_COUNTER_METRIC)
        .setDescription("API request load sent in bytes")
        .setUnit("one")
        .build();

    apiLatencyRecorder =
        meter
            .longValueRecorderBuilder(API_LATENCY_METRIC)
            .setDescription("API latency time")
            .setUnit("ms")
            .build();
  }

}
