package software.amazon.adot.testbed;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

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

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.xray.XRayClient;
import software.amazon.awssdk.services.xray.model.BatchGetTracesRequest;
import software.amazon.awssdk.services.xray.model.BatchGetTracesResponse;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceIdsWithXRayTests {
        private static final String TEST_IMAGE = System.getenv("TEST_IMAGE") != null && !System.getenv("TEST_IMAGE").isEmpty()
        ? System.getenv("TEST_IMAGE")
        : "public.ecr.aws/aws-observability/aws-otel-collector:latest";
    private final Logger collectorLogger = LoggerFactory.getLogger("collector");
    private GenericContainer<?> collector;

    private GenericContainer<?> createAndStartCollector(String configFilePath) throws IOException {

        // Create an environment variable map
        Map<String, String> envVariables = new HashMap<>();
        // Set credentials
        envVariables.put("AWS_REGION", System.getenv("AWS_REGION"));
        envVariables.put("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID"));
        envVariables.put("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY"));
        // Check if AWS_SESSION_TOKEN is not null before adding it
        if (System.getenv("AWS_SESSION_TOKEN") != null) {
            envVariables.put("AWS_SESSION_TOKEN", System.getenv("AWS_SESSION_TOKEN"));
        }

        var collector = new FixedHostPortGenericContainer<>(TEST_IMAGE)
            .withCopyFileToContainer(MountableFile.forClasspathResource(configFilePath), "/etc/collector/config.yaml")
            .withFixedExposedPort(4317, 4317, InternetProtocol.TCP)
            .withLogConsumer(new Slf4jLogConsumer(collectorLogger))
            .waitingFor(Wait.forLogMessage(".*Everything is ready. Begin running and processing data.*", 1))
            .withEnv(envVariables)
            .withCommand("--config", "/etc/collector/config.yaml");

        collector.start();
        return collector;
    }

    @Test
    void testXRayTraceIdSendToXRay() throws Exception {
        List<String> traceIds = createTraces(true);
        validateTracesInXRay(traceIds);
    }

    @Test
    void testW3CTraceIdSendToXRay() throws Exception {
        List<String> traceIds = createTraces(false);
        validateTracesInXRay(traceIds);
    }

    List<String> createTraces(boolean useXRayIDGenerator) throws Exception {
        collector = createAndStartCollector("/configurations/config-xrayExporter.yaml");

        OpenTelemetry otel = openTelemetry(useXRayIDGenerator);
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

        // Takes a few seconds for traces to appear in XRay
        Thread.sleep(20000);

        assertThat(traceIds).hasSize(numOfTraces);
        return traceIds;
    }

    void validateTracesInXRay(List<String> traceIds) {
        Region region = Region.of(System.getenv("AWS_REGION"));
        XRayClient xray = XRayClient.builder()
            .region(region)
            .build();
        BatchGetTracesResponse tracesResponse = xray.batchGetTraces(BatchGetTracesRequest.builder()
            .traceIds(traceIds)
            .build());

        // Assertions
        Set<String> traceIdsSet = new HashSet<String>(traceIds);
        assertThat(tracesResponse.traces()).hasSize(traceIds.size());
        tracesResponse.traces().forEach(trace -> {
            assertThat(traceIdsSet.contains(trace.id())).isTrue();
        });
    }

    @AfterEach
    public void resetGlobalOpenTelemetry() {
        // Cleanup collector and otel sdk
        collector.stop();
        GlobalOpenTelemetry.resetForTest();
    }

    public OpenTelemetry openTelemetry(boolean useXRayIDGenerator) {
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
            
        if (useXRayIDGenerator) {
            tracerProvider = tracerProviderBuilder
                .setIdGenerator(AwsXrayIdGenerator.getInstance())
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
