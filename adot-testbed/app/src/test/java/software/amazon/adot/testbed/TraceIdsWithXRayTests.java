package software.amazon.adot.testbed;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.contrib.awsxray.AwsXrayIdGenerator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.IdGenerator;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.xray.XRayClient;
import software.amazon.awssdk.services.xray.model.BatchGetTracesRequest;
import software.amazon.awssdk.services.xray.model.BatchGetTracesResponse;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceIdsWithXRayTests extends CollectorSetup {
    private static final IdGenerator XRAY_ID_GENERATOR = AwsXrayIdGenerator.getInstance();

    @Test
    void testXRayTraceIdSendToXRay() throws Exception {
        List<String> traceIds = createTraces(XRAY_ID_GENERATOR);
        validateTracesInXRay(traceIds);
    }

    @Test
    void testW3CTraceIdSendToXRay() throws Exception {
        List<String> traceIds = createTraces(null);
        validateTracesInXRay(traceIds);
    }

    List<String> createTraces(IdGenerator idGenerator) throws Exception {
        collector = createAndStartCollectorForXray("/configurations/config-xrayExporter.yaml");

        OpenTelemetry otel = openTelemetry(idGenerator);
        Tracer tracer = otel.getTracer("adot-trace-test");

        Attributes attributes = Attributes.of(
            AttributeKey.stringKey("http.method"), "GET",
            AttributeKey.stringKey("http.url"), "http://localhost:8080/randomEndpoint"
        );

        int numOfTraces = 5;
        List<String> traceIds = new ArrayList<String>();
        for (int count = 0; count < numOfTraces; count++) {
            Span span = tracer.spanBuilder("trace-id-test")
                .setSpanKind(SpanKind.SERVER)
                .setAllAttributes(attributes)
                .startSpan();
            //Format trace IDs to XRay format ({16 bytes} --> {1-<4-bytes>-<12-bytes>})
            String id = new StringBuilder(span.getSpanContext().getTraceId()).insert(8, "-").insert(0, "1-").toString();
            traceIds.add(id);
            span.end();
        }

        assertThat(traceIds).hasSize(numOfTraces);
        return traceIds;
    }

    void validateTracesInXRay(List<String> traceIds) throws Exception {
        Region region = Region.of(System.getenv("AWS_REGION"));
        XRayClient xray = XRayClient.builder()
            .region(region)
            .build();

        RetryerBuilder.<Void>newBuilder()
            .retryIfException()
            .retryIfRuntimeException()
            .retryIfExceptionOfType(java.lang.AssertionError.class)
            .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(5))
            .build()
            .call(() -> {
                BatchGetTracesResponse tracesResponse = xray.batchGetTraces(BatchGetTracesRequest.builder()
                    .traceIds(traceIds)
                    .build());

                // Assertions
                Set<String> traceIdsSet = new HashSet<String>(traceIds);
                assertThat(tracesResponse.traces()).hasSize(traceIds.size());
                tracesResponse.traces().forEach(trace -> {
                    assertThat(traceIdsSet.contains(trace.id())).isTrue();
                });

                return null;
            });
    }

    @AfterEach
    public void resetGlobalOpenTelemetry() {
        // Cleanup collector and otel sdk
        collector.stop();
        GlobalOpenTelemetry.resetForTest();
    }

    private OpenTelemetry openTelemetry(IdGenerator idGenerator) {
        Resource resource = Resource.getDefault().toBuilder()
            .put(ResourceAttributes.SERVICE_NAME, "xray-test")
            .put(ResourceAttributes.SERVICE_VERSION, "0.1.0")
            .build();

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
            .setSampler(Sampler.alwaysOn())
            .setResource(resource)
            .build();

        String exporter = System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317");

        SdkTracerProvider tracerProvider;
        SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder()
            .addSpanProcessor(
                BatchSpanProcessor.builder(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint(exporter)
                        .build())
                    .build())
            .setResource(resource);
            
        if (idGenerator != null) {
            tracerProvider = tracerProviderBuilder
                .setIdGenerator(idGenerator)
                .build();
        } else {
            tracerProvider = tracerProviderBuilder.build();
        }

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal();

        return openTelemetry;
    }
}
