package com.amazon.opentelemetry.load.generator.factory;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.aws.trace.AwsXrayIdGenerator;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.spi.OpenTelemetryFactory;

public class AwsTracerProviderFactory implements OpenTelemetryFactory {
  private static final TracerSdkProvider TRACER_PROVIDER;
  static {
    if (System.getProperty("otel.aws.imds.endpointOverride") == null) {
      String overrideFromEnv = System.getenv("OTEL_AWS_IMDS_ENDPOINT_OVERRIDE");
      if (overrideFromEnv != null) {
        System.setProperty("otel.aws.imds.endpointOverride", overrideFromEnv);
      }
    }
    TRACER_PROVIDER =
        TracerSdkProvider.builder().setIdGenerator(new AwsXrayIdGenerator()).build();
  }
  @Override
  public OpenTelemetrySdk create() {
    return OpenTelemetrySdk.builder().setTracerProvider(TRACER_PROVIDER).build();
  }
}
