package software.amazon.adot.testbed;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Files;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.InternetProtocol;

abstract class CollectorSetup {
    private static final String TEST_IMAGE = System.getenv("TEST_IMAGE") != null && !System.getenv("TEST_IMAGE").isEmpty()
        ? System.getenv("TEST_IMAGE")
        : "public.ecr.aws/aws-observability/aws-otel-collector:latest";
    private final Logger collectorLogger = LoggerFactory.getLogger("collector");
    protected Path logDirectory;
    protected GenericContainer<?> collector;

    private Map<String, String> createCollectorEnvVars(String logStreamName) {
        // Create an environment variable map
        Map<String, String> envVariables = new HashMap<>();
        if (logStreamName != null) {
            envVariables.put("LOG_STREAM_NAME", logStreamName);
        }
        //Set credentials
        envVariables.put("AWS_REGION", System.getenv("AWS_REGION"));
        envVariables.put("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID"));
        envVariables.put("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY"));
        // Check if AWS_SESSION_TOKEN is not null before adding it
        if (System.getenv("AWS_SESSION_TOKEN") != null) {
            envVariables.put("AWS_SESSION_TOKEN", System.getenv("AWS_SESSION_TOKEN"));
        }
        return envVariables;
    }

    protected GenericContainer<?> createAndStartCollectorForLogs(String configFilePath, String logStreamName) throws IOException {
        try {
            logDirectory = Files.createTempDirectory("tempLogs");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory", e);
        }
        var collector = new GenericContainer<>(TEST_IMAGE)
            .withCopyFileToContainer(MountableFile.forClasspathResource(configFilePath), "/etc/collector/config.yaml")
            .withLogConsumer(new Slf4jLogConsumer(collectorLogger))
            .waitingFor(Wait.forLogMessage(".*Everything is ready. Begin running and processing data.*", 1))
            .withEnv(createCollectorEnvVars(logStreamName))
            .withCreateContainerCmdModifier(cmd -> cmd.withUser("root"))
            .withClasspathResourceMapping("/logs", "/logs", BindMode.READ_WRITE)
            .withCommand("--config", "/etc/collector/config.yaml");

        //Mount the Temp directory
        collector.withFileSystemBind(logDirectory.toString(),"/tempLogs", BindMode.READ_WRITE);

        collector.start();
        return collector;
    }

    protected GenericContainer<?> createAndStartCollectorForXray(String configFilePath) throws IOException {
        var collector = new FixedHostPortGenericContainer<>(TEST_IMAGE)
            .withCopyFileToContainer(MountableFile.forClasspathResource(configFilePath), "/etc/collector/config.yaml")
            .withFixedExposedPort(4317, 4317, InternetProtocol.TCP)
            .withLogConsumer(new Slf4jLogConsumer(collectorLogger))
            .waitingFor(Wait.forLogMessage(".*Everything is ready. Begin running and processing data.*", 1))
            .withEnv(createCollectorEnvVars(null))
            .withCommand("--config", "/etc/collector/config.yaml");

        collector.start();
        return collector;
    }
}
