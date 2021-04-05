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

import brave.Tracing;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import com.amazon.opentelemetry.trace.model.topology.LoadGeneratorParams;
import com.amazon.opentelemetry.trace.model.trace.Service;
import com.amazon.opentelemetry.trace.model.trace.Trace;
import com.amazon.opentelemetry.trace.utils.OpenTracingTraceConverter;
import com.amazon.opentelemetry.trace.utils.TraceGenerator;
import com.amazon.opentelemetry.trace.utils.TraceTraversal;
import brave.opentracing.BraveTracer;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import zipkin2.codec.Encoding;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;
import brave.propagation.B3Propagation;

public class ZipkinTraceEmitClient implements TraceClient {

  private String collectorUrl;
  private LoadGeneratorParams genParams;

  private final Map<String, BraveTracer> serviceNameToTracer = new HashMap<>();
  private static final String V2_API = "/api/v2/spans";
  private static final String V1_API = "/api/v1/spans";
  private SpanBytesEncoder spanBytesEncoder = SpanBytesEncoder.THRIFT;

  public ZipkinTraceEmitClient(String collectorUrl) throws Exception {
    this.genParams = this.initGenerator();
    this.collectorUrl = normalizeUrl(collectorUrl);
  }



  @Override
  public String emit() throws Exception {
    long now = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());
    Trace trace = TraceGenerator.generate(this.genParams, now);
    return this.emit(trace);
  }

  public String emit(Trace trace) {
    final MutableObject<String> traceId = new MutableObject<>(null);
    final Map<UUID, Span> convertedSpans = new HashMap<>();
    Consumer<com.amazon.opentelemetry.trace.model.trace.Span> createOtSpan = span -> {
      boolean extract = StringUtils.isEmpty(traceId.getValue());
      BraveTracer tracer = getTracer(span.service);
      io.opentracing.Span otSpan = OpenTracingTraceConverter.createOTSpan(
          tracer, span, convertedSpans
      );
      convertedSpans.put(span.id, otSpan);
      if (extract) {
        traceId.setValue(extractTraceID(tracer, otSpan));
      }
    };
    Consumer<com.amazon.opentelemetry.trace.model.trace.Span> closeOtSpan = span -> {
      // mark span as closed
      convertedSpans.get(span.id).finish(span.endTimeMicros);
    };
    TraceTraversal.prePostOrder(trace, createOtSpan, closeOtSpan);
    return traceId.getValue();
  }

  private String extractTraceID(Tracer tracer, Span otSpan) {
    HashMap<String, String> baggage = new HashMap<>();
    TextMapInjectAdapter map = new TextMapInjectAdapter(baggage);
    tracer.inject(otSpan.context(), Format.Builtin.HTTP_HEADERS, map);
    try {
      String encodedTraceId = URLDecoder.decode(baggage.get("X-B3-TraceId"), "UTF-8");
      return encodedTraceId.split(":")[0];
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

  private BraveTracer getTracer(Service service) {
    return this.serviceNameToTracer.computeIfAbsent(service.serviceName,
        s -> createBraveTracer(collectorUrl, service));
  }

  private BraveTracer createBraveTracer(String collectorUrl, Service svc) {
    Encoding encoding = Encoding.JSON;
    String queryPath = V2_API;
    switch (this.spanBytesEncoder) {
      case JSON_V1:
        queryPath = V1_API;
        break;
      case THRIFT:
        encoding = Encoding.THRIFT;
        queryPath = V1_API;
        break;
      case JSON_V2:
        break;
      case PROTO3:
        encoding = Encoding.PROTO3;
        break;
    }

    Sender sender = OkHttpSender.newBuilder().encoding(encoding).endpoint(collectorUrl + queryPath).build();
    Reporter<zipkin2.Span> spanReporter = AsyncReporter.builder(sender).build(this.spanBytesEncoder);
    Propagation.Factory propagationFactory = B3Propagation.FACTORY;
    Tracing.Builder braveTracingB = Tracing.newBuilder()
        .localServiceName(svc.serviceName)
        .propagationFactory(propagationFactory)
        .spanReporter(spanReporter)
        .sampler(Sampler.ALWAYS_SAMPLE);
    Tracing braveTracing = braveTracingB.build();
    return BraveTracer.create(braveTracing);
  }
}
