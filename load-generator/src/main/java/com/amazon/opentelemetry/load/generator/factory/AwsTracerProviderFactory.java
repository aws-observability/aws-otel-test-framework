package com.amazon.opentelemetry.load.generator.factory;

import io.opentelemetry.sdk.extension.trace.aws.AwsXrayIdGenerator;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.api.trace.spi.TracerProviderFactory;


public class AwsTracerProviderFactory implements TracerProviderFactory {
  private static final TracerSdkProvider TRACER_PROVIDER;
  static {
    if (System.getProperty("otel.aws.imds.endpointOverride") == null) {
      String overrideFromEnv = System.getenv("OTEL_AWS_IMDS_ENDPOINT_OVERRIDE");
      if (overrideFromEnv != null) {
        System.setProperty("otel.aws.imds.endpointOverride", overrideFromEnv);
      }
    }
    TRACER_PROVIDER =
        TracerSdkProvider.builder().setIdsGenerator(new AwsXrayIdGenerator()).build();
  }
  @Override
  public TracerProvider create() {
    return TRACER_PROVIDER;
  }
}
