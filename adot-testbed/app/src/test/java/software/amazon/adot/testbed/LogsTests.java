package software.amazon.adot.testbed;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogsTests {
    private static final String TEST_IMAGE = System.getenv("TEST_IMAGE") != null && !System.getenv("TEST_IMAGE").isEmpty()
        ? System.getenv("TEST_IMAGE")
        : "public.ecr.aws/aws-observability/aws-otel-collector:latest";
    private final Logger collectorLogger = LoggerFactory.getLogger("collector");
    private static final String uniqueID = UUID.randomUUID().toString();

    private GenericContainer<?> collector;

    private GenericContainer<?> createAndStartCollector(String configFilePath, String logFilePath, String logStreamName) throws IOException {

        // Create an environment variable map
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("LOG_STREAM_NAME", logStreamName);
        //Set credentials
        envVariables.put("AWS_REGION", System.getenv("AWS_REGION"));
        envVariables.put("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID"));
        envVariables.put("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY"));
        // Check if AWS_SESSION_TOKEN is not null before adding it
        if (System.getenv("AWS_SESSION_TOKEN") != null) {
            envVariables.put("AWS_SESSION_TOKEN", System.getenv("AWS_SESSION_TOKEN"));
        }

        var collector = new GenericContainer<>(TEST_IMAGE)
            .withCopyFileToContainer(MountableFile.forClasspathResource(configFilePath), "/etc/collector/config.yaml")
            .withLogConsumer(new Slf4jLogConsumer(collectorLogger))
            .waitingFor(Wait.forLogMessage(".*Everything is ready. Begin running and processing data.*", 1))
            .withCommand("--config", "/etc/collector/config.yaml", "--feature-gates=+adot.filelog.receiver,+adot.awscloudwatchlogs.exporter,+adot.file_storage.extension")
            .withEnv(envVariables);

        //Mount the log file for the file log receiver to parse
        collector.withCopyFileToContainer(MountableFile.forClasspathResource(logFilePath), logFilePath );

        collector.start();
        collector.waitingFor(Wait.forHealthcheck());
        return collector;
    }

    @Test
    void testSyslog() throws Exception {
        String logStreamName = "rfcsyslog-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-rfcsyslog.yaml", "/logs/RFC5424.log", logStreamName);

        validateLogs(logStreamName , "/logs/RFC5424.log");
        collector.stop();
    }

    @Test
    void testLog4j() throws Exception {
        String logStreamName = "log4j-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-log4j.yaml", "/logs/log4j.log", logStreamName);

        validateLogs(logStreamName , "/logs/log4j.log");
        collector.stop();
    }

    @Test
    void testJson() throws Exception {
        String logStreamName = "json-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-json.yaml", "/logs/testingJSON.log", logStreamName);

        validateLogs(logStreamName , "/logs/testingJSON.log");
        collector.stop();
    }

    void validateLogs(String testLogStreamName, String logFilePath) throws Exception {
        var file = new File(logFilePath);
        var lines = new HashSet<String>();

        try (InputStream inputStream = getClass().getResourceAsStream(logFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading from the file: " + logFilePath, e);
        }

        var cwClient = CloudWatchLogsClient.builder()
            .build();

        var objectMapper = new ObjectMapper();

        RetryerBuilder.<Void>newBuilder()
            .retryIfException()
            .retryIfRuntimeException()
            .retryIfExceptionOfType(org.opentest4j.AssertionFailedError.class)
            .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(5))
            .build()
            .call(() -> {
                var now = Instant.now();
                var start = now.minus(Duration.ofMinutes(2));
                var end = now.plus(Duration.ofMinutes(2));
                var response = cwClient.getLogEvents(GetLogEventsRequest.builder().logGroupName("adot-testbed/logs-component-testing/logs")
                    .logStreamName(testLogStreamName)
                    .startTime(start.toEpochMilli())
                    .endTime(end.toEpochMilli())
                    .build());

                var events = response.events();
                var receivedMessages = events.stream().map(x -> x.message()).collect(Collectors.toSet());

                // Extract the "body" field from each received message that is received from cloudwatch in JSON Format
                var messageToValidate = receivedMessages.stream()
                    .map(message -> {
                        try {
                            JsonNode jsonNode = objectMapper.readTree(message);
                            return jsonNode.get("body").asText();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

                //Validate body field in JSON-messageToValidate with actual log line from the log file.
                assertThat(messageToValidate.containsAll(lines)).isTrue();
                return null;
            });
    }

}
