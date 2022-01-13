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

package com.amazon.opentelemetry.trace.client;

import com.amazon.opentelemetry.trace.model.topology.LoadGeneratorParams;
import com.amazon.opentelemetry.trace.model.trace.KeyValue;
import com.amazon.opentelemetry.trace.model.trace.Service;
import com.amazon.opentelemetry.trace.model.trace.Span;
import com.amazon.opentelemetry.trace.model.trace.Trace;
import com.amazon.opentelemetry.trace.utils.OpenTracingTraceConverter;
import com.amazon.opentelemetry.trace.utils.TraceGenerator;
import com.amazon.opentelemetry.trace.utils.TraceTraversal;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.thrift.transport.TTransportException;

public class JaegerTraceEmitClient implements TraceClient {

  private LoadGeneratorParams genParams;
  private String collectorUrl;

  private final int flushIntervalMillis = 1000;
  private final Map<String, Tracer> serviceNameToTracer = new HashMap<>();

  public JaegerTraceEmitClient(String collectorUrl) throws Exception {
    this.genParams = this.initGenerator();
    this.collectorUrl = normalizeUrl(collectorUrl);;
  }

  @Override
  public String emit() throws Exception{
    long now = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());
    Trace trace = TraceGenerator.generate(this.genParams, now);
    return this.emit(trace);
  }

  public String emit(Trace trace) {
    final MutableObject<String> traceId = new MutableObject<>(null);
    final Map<UUID, io.opentracing.Span> convertedSpans = new HashMap<>();
    Consumer<Span> createOtSpan = span -> {
      boolean extract = StringUtils.isEmpty(traceId.getValue());
      Tracer tracer = getTracer(span.service);
      io.opentracing.Span otSpan = OpenTracingTraceConverter.createOTSpan(
          tracer, span, convertedSpans
      );
      convertedSpans.put(span.id, otSpan);
      if (extract) {
        traceId.setValue(extractTraceId(tracer, otSpan));
      }
    };
    Consumer<Span> closeOtSpan = span -> {
      // mark span as closed
      convertedSpans.get(span.id).finish(span.endTimeMicros);
    };
    TraceTraversal.prePostOrder(trace, createOtSpan, closeOtSpan);
    return traceId.getValue();
  }

  private Tracer getTracer(Service service) {
    return this.serviceNameToTracer.computeIfAbsent(service.serviceName,
        s -> createJaegerTracer(collectorUrl, service, flushIntervalMillis));
  }

  private static Tracer createJaegerTracer(String collectorUrl, Service svc, int flushIntervalMillis) {
    HttpSender sender = null;
    try {
      sender = new HttpSender.Builder(collectorUrl + "/api/traces").build();
    } catch (TTransportException e) {
      throw new RuntimeException("Exception when trying to create sender " + e.getMessage());
    }
    Reporter reporter = new RemoteReporter.Builder().withSender(sender)
            .withMaxQueueSize(100000)
            .withFlushInterval(flushIntervalMillis)
            .build();
    JaegerTracer.Builder bld = new JaegerTracer.Builder(svc.serviceName).withReporter(reporter)
            .withSampler(new ConstSampler(true));
    for (KeyValue kv : svc.tags) {
      bld.withTag(kv.key, kv.valueString);
    }
    return bld.build();
  }

  /**
   * This extracts the jaeger-header traceID from an opentracing span.
   *
   * @param tracer tracer to use to extract the jaeger header trace id
   * @param otSpan span from which to extract the traceID
   * @return string traceID or null if could not decode
   */
  private String extractTraceId(Tracer tracer, io.opentracing.Span otSpan) {
    HashMap<String, String> baggage = new HashMap<>();
    TextMapAdapter map = new TextMapAdapter(baggage);
    tracer.inject(otSpan.context(), Format.Builtin.HTTP_HEADERS, map);
    try {
      String encodedTraceId = URLDecoder.decode(baggage.get("uber-trace-id"), "UTF-8");
      return encodedTraceId.split(":")[0];
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }
}
