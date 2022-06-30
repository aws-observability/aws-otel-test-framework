package com.amazon.sampleapp;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.AwsXrayIdGenerator;
import io.opentelemetry.contrib.awsxray.AwsXrayRemoteSampler;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.extension.aws.AwsXrayPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

  public final static Resource RESOURCE = Resource.builder().build();

  // Opentelemetry builder to create a xray remote sampler with polling interval of 1
  public final static OpenTelemetry OPEN_TELEMETRY =
      OpenTelemetrySdk.builder()
          .setPropagators(
              ContextPropagators.create(
                  TextMapPropagator.composite(
                      W3CTraceContextPropagator.getInstance(), AwsXrayPropagator.getInstance())))
          .setTracerProvider(
              SdkTracerProvider.builder()
                  .addSpanProcessor(
                      BatchSpanProcessor.builder(OtlpGrpcSpanExporter.getDefault()).build())
                  .setResource(RESOURCE)
                  .setSampler(
                      AwsXrayRemoteSampler.newBuilder(RESOURCE)
                          .setPollingInterval(Duration.ofSeconds(1))
                          .build())
                  .setIdGenerator(AwsXrayIdGenerator.getInstance())
                  .build())
          .buildAndRegisterGlobal();

  public static void main(String[] args) {
    // listenAddress should consist host + port (e.g. 127.0.0.1:5000)
    String port;
    String host;
    String listenAddress = System.getenv("LISTEN_ADDRESS");

    if (listenAddress == null) {
      host = "127.0.0.1";
      port = "8080";
    } else {
      String[] splitAddress = listenAddress.split(":");
      host = splitAddress[0];
      port = splitAddress[1];
    }

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("server.address", host);
    config.put("server.port", port);

    SpringApplication app = new SpringApplication(Application.class);
    app.setDefaultProperties(config);
    app.run(args);
  }
}
