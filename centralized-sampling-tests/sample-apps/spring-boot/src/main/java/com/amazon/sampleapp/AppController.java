package com.amazon.sampleapp;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

// Controller for the application
@Controller
public class AppController {

  // Get endpoint for /getSampled that requires three header values for user, service_name, and
  // required
  // Returns the number of times a span was sampled out of the creation of 1000 spans
  @GetMapping(value = "/getSampled")
  @ResponseBody
  public int getSampled(
      @RequestHeader("user") String userAttribute,
      @RequestHeader("service_name") String name,
      @RequestHeader("required") String required,
      @RequestHeader("totalSpans") String totalSpans) {
    int numSampled = 0;
    int spans = Integer.parseInt(totalSpans);
    for (int i = 0; i < spans; i++) {
      Attributes attributes =
          Attributes.of(
              AttributeKey.stringKey("http.method"), "GET",
              AttributeKey.stringKey("http.url"), "http://localhost:8080/getSampled",
              AttributeKey.stringKey("user"), userAttribute,
              AttributeKey.stringKey("http.route"), "/getSampled",
              AttributeKey.stringKey("required"), required,
              AttributeKey.stringKey("http.target"), "/getSampled");
      Tracer tracer = Application.openTelemetry.getTracer(name);
      Span span =
          tracer
              .spanBuilder(name)
              .setSpanKind(SpanKind.SERVER)
              .setAllAttributes(attributes)
              .startSpan();

      boolean isSampled = span.getSpanContext().isSampled();
      span.end();
      if (isSampled) {
        numSampled++;
      }
    }
    return numSampled;
  }

  // Post endpoint for /getSampled that requires three header values for user, service_name, and
  // required
  // Returns the number of times a span was sampled out of the creation of 1000 spans
  @PostMapping("/getSampled")
  @ResponseBody
  public int postSampled(
      @RequestHeader("user") String userAttribute,
      @RequestHeader("service_name") String name,
      @RequestHeader("required") String required,
      @RequestHeader("totalSpans") String totalSpans) {

    int numSampled = 0;
    int spans = Integer.parseInt(totalSpans);
    for (int i = 0; i < spans; i++) {
      Attributes attributes =
          Attributes.of(
              AttributeKey.stringKey("http.method"), "POST",
              AttributeKey.stringKey("http.url"), "http://localhost:8080/getSampled",
              AttributeKey.stringKey("user"), userAttribute,
              AttributeKey.stringKey("http.route"), "/getSampled",
              AttributeKey.stringKey("required"), required,
              AttributeKey.stringKey("http.target"), "/getSampled");
      Tracer tracer = Application.openTelemetry.getTracer(name);
      Span span =
          tracer
              .spanBuilder(name)
              .setSpanKind(SpanKind.SERVER)
              .setAllAttributes(attributes)
              .startSpan();

      boolean isSampled = span.getSpanContext().isSampled();
      if (isSampled) {
        numSampled++;
      }
      span.end();
    }
    return numSampled;
  }

  // Get endpoint for /importantEndpoint that requires three header values for user, service_name,
  // and required
  // Returns the number of times a span was sampled out of the creation of 1000 spans
  @GetMapping("/importantEndpoint")
  @ResponseBody
  public int importantEndpoint(
      @RequestHeader("user") String userAttribute,
      @RequestHeader("service_name") String name,
      @RequestHeader("required") String required,
      @RequestHeader("totalSpans") String totalSpans) {

    int numSampled = 0;
    int spans = Integer.parseInt(totalSpans);

    for (int i = 0; i < spans; i++) {
      Attributes attributes =
          Attributes.of(
              AttributeKey.stringKey("http.method"), "GET",
              AttributeKey.stringKey("http.url"), "http://localhost:8080/importantEndpoint",
              AttributeKey.stringKey("http.route"), "/importantEndpoint",
              AttributeKey.stringKey("required"), required,
              AttributeKey.stringKey("user"), userAttribute,
              AttributeKey.stringKey("http.target"), "/importantEndpoint");
      Tracer tracer = Application.openTelemetry.getTracer(name);
      Span span =
          tracer
              .spanBuilder(name)
              .setSpanKind(SpanKind.SERVER)
              .setAllAttributes(attributes)
              .startSpan();

      boolean isSampled = span.getSpanContext().isSampled();
      if (isSampled) {
        numSampled++;
      }
      span.end();
    }
    return numSampled;
  }
}
