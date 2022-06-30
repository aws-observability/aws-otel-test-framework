package com.amazon.sampleapp;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

// Controller for the application
@Controller
public class AppController {
    // Global variable to represent number of spans the endpoint creates
    public static int totalSpans = 1000;

    // Get endpoint for /getSampled that requires three header values for user, service_name, and required
    // Returns the number of times a span was sampled out of the creation of 1000 spans
    @GetMapping(value = "/getSampled")
    @ResponseBody
    public String getSampled(
            @RequestHeader("user") String userAttribute,
            @RequestHeader("service_name") String name,
            @RequestHeader("required") String required) {

        int numSampled = 0;
        for (int i = 0; i < totalSpans; i++)
        {
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
            span.setAttribute("http.status_code", 200);
            span.setAttribute("http.client_ip", "127.0.0.1");

            boolean isSampled = span.getSpanContext().isSampled();
            span.end();
            if (isSampled){
                numSampled++;
            }
        }
        return String.valueOf(numSampled);
    }

    // Post endpoint for /getSampled that requires three header values for user, service_name, and required
    // Returns the number of times a span was sampled out of the creation of 1000 spans
    @PostMapping("/getSampled")
    @ResponseBody
    public String postSampled(
            @RequestHeader("user") String userAttribute,
            @RequestHeader("service_name") String name,
            @RequestHeader("required") String required) {

        int numSampled = 0;
        for (int i = 0; i < totalSpans; i++) {
            Attributes attributes =
                    Attributes.of(
                            AttributeKey.stringKey("http.method"), "POST",
                            AttributeKey.stringKey("http.url"), "http://localhost:8080/getSampled",
                            AttributeKey.stringKey("user"), userAttribute,
                            AttributeKey.stringKey("http.route"), "/getSampled",
                            AttributeKey.stringKey("required"), required,
                            AttributeKey.stringKey("http.target"), "/getSampled");
            Tracer tracer = Application.openTelemetry.getTracer("/postSampled");
            Span span =
                    tracer
                            .spanBuilder("/postSampled")
                            .setSpanKind(SpanKind.SERVER)
                            .setAllAttributes(attributes)
                            .startSpan();
            span.setAttribute("http.status_code", 200);
            span.setAttribute("http.client_ip", "127.0.0.1");

            boolean isSampled = span.getSpanContext().isSampled();
            if (isSampled){
                numSampled++;
            }
            span.end();
        }
        return String.valueOf(numSampled);
    }

    // Get endpoint for /importantEndpoint that requires three header values for user, service_name, and required
    // Returns the number of times a span was sampled out of the creation of 1000 spans
    @GetMapping("/importantEndpoint")
    @ResponseBody
    public String importantEndpoint(
            @RequestHeader("user") String userAttribute,
            @RequestHeader("service_name") String name,
            @RequestHeader("required") String required) {

        int numSampled = 0;
        for (int i = 0; i < totalSpans; i++) {
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
            span.setAttribute("http.status_code", 200);
            span.setAttribute("http.client_ip", "127.0.0.1");

            boolean isSampled = span.getSpanContext().isSampled();
            if (isSampled) {
                numSampled++;
            }
            span.end();
        }
        return String.valueOf(numSampled);
    }
}
