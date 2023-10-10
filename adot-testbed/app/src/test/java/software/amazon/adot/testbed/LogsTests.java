package software.amazon.adot.testbed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogsTests {
    private static final String TEST_IMAGE = System.getenv("TEST_IMAGE") != null && !System.getenv("TEST_IMAGE").isEmpty()
        ? System.getenv("TEST_IMAGE")
        : "public.ecr.aws/aws-observability/aws-otel-collector:latest";
    private final Logger collectorLogger = LoggerFactory.getLogger("collector");
    private static final String uniqueID = UUID.randomUUID().toString();
    private final Path logDirectory = Files.createTempDirectory("filerotationlogs");
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
//        envVariables.put("TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED", "true");

        var collector = new GenericContainer<>(TEST_IMAGE)
            .withCopyFileToContainer(MountableFile.forClasspathResource(configFilePath), "/etc/collector/config.yaml")
            .withLogConsumer(new Slf4jLogConsumer(collectorLogger))
            .waitingFor(Wait.forLogMessage(".*Everything is ready. Begin running and processing data.*", 1))
            .withEnv(envVariables)
            .withClasspathResourceMapping("/logs", "/logs", BindMode.READ_WRITE)

            .withPrivilegedMode(true)
            .withCommand("--config", "/etc/collector/config.yaml", "--feature-gates=+adot.filelog.receiver,+adot.awscloudwatchlogs.exporter,+adot.file_storage.extension");

        //Mount the log file for the file log receiver to parse
        collector.withCopyFileToContainer(MountableFile.forClasspathResource(logFilePath), logFilePath);
        collector.withFileSystemBind(logDirectory.toString(),"/filerotation", BindMode.READ_WRITE);
//        collector.withFileSystemBind(tempFile.getAbsolutePath(), "/filerotation", BindMode.READ_WRITE);
//        collector.withFileSystemBind(tempFileB.getAbsolutePath(), "/logs/filerotationlogs/logsB.log", BindMode.READ_WRITE);
//        collector.withCopyFileToContainer(MountableFile.forClasspathResource("/logs/"), "/logs/",  );
//        collector.withCommand("sh", "-c", "chmod -R a+rw " + "/logs");

        collector.start();
        collector.waitingFor(Wait.forHealthcheck());
        return collector;
    }

    @Test
    void testSyslog() throws Exception {
        String logStreamName = "rfcsyslog-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-rfcsyslog.yaml", "/logs/RFC5424.log", logStreamName);

        List<String> logFilePaths = new ArrayList<>();
        logFilePaths.add("/logs/testingJSON.log");
        validateLogs(logStreamName , logFilePaths, true);
        collector.stop();
    }

    @Test
    void testLog4j() throws Exception {
        String logStreamName = "log4j-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-log4j.yaml", "/logs/log4j.log", logStreamName);

        List<String> logFilePaths = new ArrayList<>();
        logFilePaths.add("/logs/testingJSON.log");
        validateLogs(logStreamName , logFilePaths, true);
        collector.stop();
    }

    @Test
    void testJson() throws Exception {
        String logStreamName = "json-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-json.yaml", "/logs/testingJSON.log", logStreamName);

        List<String> logFilePaths = new ArrayList<>();
        logFilePaths.add("/logs/testingJSON.log");
        validateLogs(logStreamName , logFilePaths, true);
        collector.stop();
    }

    @Test
    void testCollectorRestartAfterCrash() throws Exception {
        String logStreamName = "storageExtension-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-storageExtension.yaml", "/logs/storageExtension.log", logStreamName);
        String resourceFilePath = "/logs/storageExtension.log"; // Path to the resource file
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        try (OutputStream outputStream = new FileOutputStream(new File(getClass().getResource(resourceFilePath).toURI()), true);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            // Append the lines to the file
            writer.write("[otel.javaagent 2023-09-28 16:56:22:242 +0000] [OkHttp ConnectionPool] DEBUG okhttp3.internal.concurrent.TaskRunner - Q10002 run again after 300 s : OkHttp ConnectionPool- First Entry");
            writer.newLine();
            writer.write("[otel.javaagent 2023-09-29 16:56:22:242 +0000] [OkHttp ConnectionPool] DEBUG okhttp3.internal.concurrent.TaskRunner - Q10002 run again after 300 s : OkHttp ConnectionPool- Second Entry");
            writer.newLine();
            writer.write("[otel.javaagent 2023-09-29 16:56:22:242 +0000] [OkHttp ConnectionPool] DEBUG okhttp3.internal.concurrent.TaskRunner - Q10002 run again after 300 s : OkHttp ConnectionPool- Third Entry");
            writer.newLine();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error writing to the file: " + resourceFilePath, e);
        }

        List<String> logFilePaths = new ArrayList<>();
        logFilePaths.add(resourceFilePath);
        validateLogs(logStreamName , logFilePaths, true);
        collector.stop();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        try (OutputStream outputStream = new FileOutputStream(new File(getClass().getResource(resourceFilePath).toURI()), true);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            // Append the lines to the file
            writer.write("[otel.javaagent 2023-09-28 16:56:22:242 +0000] [OkHttp ConnectionPool] DEBUG okhttp3.internal.concurrent.TaskRunner - Q10002 run again after 300 s : OkHttp ConnectionPool- Fourth Entry");
            writer.newLine();
            writer.write("[otel.javaagent 2023-09-29 16:56:22:242 +0000] [OkHttp ConnectionPool] DEBUG okhttp3.internal.concurrent.TaskRunner - Q10002 run again after 300 s : OkHttp ConnectionPool- Fifth Entry");
            writer.newLine();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error writing to the file: " + resourceFilePath, e);
        }

        collector.start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        validateLogs(logStreamName , logFilePaths, true);
        collector.stop();
    }

    @Test
    void testFileRotation() throws Exception {
        String logStreamName = "fileRotation-logstream-" + uniqueID;
        String resourceFilePath = "/logs/filerotationlogs/log4j.log"; // Path to the resource file
        collector = createAndStartCollector("/configurations/config-fileRotation.yaml", resourceFilePath, logStreamName);

        Thread.sleep(10000);

        // Create and write data to File A
        File tempFile = new File(logDirectory.toString(), "testlogA.log");
        try (FileWriter fileWriter = new FileWriter(tempFile)) {
            fileWriter.write("Message in File A" + "\n");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("Error writing to File A.", e);
        }
        Thread.sleep(10000);

        File renameFile = new File(logDirectory.toString(), "testlogA-1234.log");
        tempFile.renameTo(renameFile);

        Thread.sleep(10000);

        File tempFileB = new File(logDirectory.toString(), "testlogA.log");
        try (FileWriter newfileWriter = new FileWriter(tempFileB)) {
            newfileWriter.write("Message in renamed file - line 1" + "\n");
            newfileWriter.write("Message in renamed file - line 2" + "\n");
            newfileWriter.write("Message in renamed file - line 3" + "\n");
            newfileWriter.flush();
            newfileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("Error writing to File B.", e);
        }
        Thread.sleep(10000);

        List<String> logFilePaths = new ArrayList<>();
        String expectedLogPath = logDirectory.toString();
        logFilePaths.add(expectedLogPath + "/testlogA-1234.log");
        logFilePaths.add(expectedLogPath + "/testlogA.log");

        validateLogs(logStreamName, logFilePaths, false);

        collector.stop();
    }


    void validateLogs(String testLogStreamName, List<String> logFilePaths, boolean areResourceFiles) throws Exception {
        var lines = new HashSet<String>();

        for (String logFilePath : logFilePaths) {
            InputStream inputStream;
            if (areResourceFiles) {
                inputStream = getClass().getResourceAsStream(logFilePath);
            } else {
                inputStream = new FileInputStream(logFilePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading from the file: " + logFilePath, e);
            }
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

                System.out.println("Log lines From Cloudwatch");
                messageToValidate.forEach(System.out::println);

                System.out.println("Expected logs");
                lines.forEach(System.out::println);
                //Validate body field in JSON-messageToValidate with actual log line from the log file.
                assertThat(messageToValidate.containsAll(lines)).isTrue();
                assertThat(messageToValidate).containsExactlyInAnyOrderElementsOf(lines);
                return null;
            });
    }

}
