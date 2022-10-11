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
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.time.Duration;

@Log4j2
public class OtlpMetricEmitter extends MetricEmitter {

  private final static String API_NAME = "loadTest";
  private final static String DataPointID = "datapoint_id";
  private List<LongCounter> counters;
  private List<Long> gaugeValues;


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
    log.info("Updating metrics for : {}" , param.getMetricType());

    switch (param.getMetricType()) {
      case "counter":
        for(LongCounter counter: counters) {
          for(int id=0 ; id < param.getDatapointCount(); id++) {
            Attributes datapointAttributes = getDataPointAttributes(id);
            counter.add(ThreadLocalRandom.current().nextInt(1,10), datapointAttributes);
          }
        }
        break;
      case "gauge":
        for(int i=0 ; i < param.getMetricCount(); i++) {
          for (int id = 0; id < param.getDatapointCount(); id++) {
            this.gaugeValues.add(id, ThreadLocalRandom.current().nextLong(-100, 100));
          }
        }
        break;
    }
  }

  private Attributes getDataPointAttributes(int id) {
    Attributes atr = Attributes.of(AttributeKey.stringKey(DataPointID),String.valueOf(id), AttributeKey.stringKey(DIMENSION_API_NAME), API_NAME,
            AttributeKey.stringKey(DIMENSION_STATUS_CODE), "200");
    return atr;
  }

  @Override
  public void setupProvider() {
    log.info("Setting up metric provider to generate metric load...");

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
    log.info("Finished Setting up metric provider.");
    switch (param.getMetricType()) {
      case "counter":
        createCounters(meter);
        break;
      case "gauge":
        createGauges(meter);
        break;
    }
  }

  private void createCounters(Meter meter) {
    if(meter!= null) {
      log.info("Registering counter metrics...");
      counters = new ArrayList<>();
      for(int id=0 ; id < param.getMetricCount(); id++) {
        counters.add(meter.counterBuilder(API_COUNTER_METRIC + id)
                .setDescription("API request load sent in bytes")
                .setUnit("one")
                .build());
      }
    } else {
      log.error("Metric provider was not found!");
    }
  }

  private void createGauges(Meter meter) {
    if(meter != null) {
      log.info("Registering gauge metrics...");
      gaugeValues = new ArrayList<Long>();
      for (int i = 0; i < param.getMetricCount(); i++) {
        meter.gaugeBuilder(API_LATENCY_METRIC + i)
                .setDescription("API latency time")
                .setUnit("ms")
                .ofLongs().buildWithCallback(measurement ->
                        {
                          for (int id = 0; id < param.getDatapointCount(); id++) {
                            Attributes datapointAttributes = getDataPointAttributes(id);
                            measurement.record(gaugeValues.get(id), datapointAttributes);
                          }
                        }
                );
      }
    } else {
      log.error("Metric provider was not found!");
    }
  }

}