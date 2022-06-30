package com.amazon.sampleapp;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.awsxray.AwsXrayRemoteSampler;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;

// Controller for the application
@Controller
public class AppController {
  // Resource used to create xray sampler
  private Resource resource;

  // Opentelemetry builder to create a xray remote sampler with polling interval of 1
  private OpenTelemetry openTelemetry;

  @Autowired
  public void setResource() {
    this.resource = Resource.builder().build();
    this.openTelemetry =
            OpenTelemetrySdk.builder()
                    .setTracerProvider(
                            SdkTracerProvider.builder()
                                    .setResource(this.resource)
                                    .setSampler(
                                            AwsXrayRemoteSampler.newBuilder(this.resource)
                                                    .setPollingInterval(Duration.ofSeconds(1))
                                                    .build())
                                    .build())
                    .buildAndRegisterGlobal();
  }
  
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

    Attributes attributes =
        Attributes.of(
            AttributeKey.stringKey("http.method"), "GET",
            AttributeKey.stringKey("http.url"), "http://localhost:8080/getSampled",
            AttributeKey.stringKey("user"), userAttribute,
            AttributeKey.stringKey("http.route"), "/getSampled",
            AttributeKey.stringKey("required"), required,
            AttributeKey.stringKey("http.target"), "/getSampled");
    return getSampledSpanCount(name, totalSpans, attributes);
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
    Attributes attributes =
        Attributes.of(
            AttributeKey.stringKey("http.method"), "POST",
            AttributeKey.stringKey("http.url"), "http://localhost:8080/getSampled",
            AttributeKey.stringKey("user"), userAttribute,
            AttributeKey.stringKey("http.route"), "/getSampled",
            AttributeKey.stringKey("required"), required,
            AttributeKey.stringKey("http.target"), "/getSampled");
    return getSampledSpanCount(name, totalSpans, attributes);
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
    Attributes attributes =
        Attributes.of(
            AttributeKey.stringKey("http.method"), "GET",
            AttributeKey.stringKey("http.url"), "http://localhost:8080/importantEndpoint",
            AttributeKey.stringKey("http.route"), "/importantEndpoint",
            AttributeKey.stringKey("required"), required,
            AttributeKey.stringKey("user"), userAttribute,
            AttributeKey.stringKey("http.target"), "/importantEndpoint");
    return getSampledSpanCount(name, totalSpans, attributes);
  }

  private int getSampledSpanCount(String name, String totalSpans, Attributes attributes) {
    int numSampled = 0;
    int spans = Integer.parseInt(totalSpans);

    for (int i = 0; i < spans; i++) {

      Tracer tracer = this.openTelemetry.getTracer(name);
      Span span =
          tracer
              .spanBuilder(name)
              .setSpanKind(SpanKind.SERVER)
              .setAllAttributes(attributes)
              .startSpan();

      if (span.getSpanContext().isSampled()) {
        numSampled++;
      }
      span.end();
    }
    return numSampled;
  }
}
