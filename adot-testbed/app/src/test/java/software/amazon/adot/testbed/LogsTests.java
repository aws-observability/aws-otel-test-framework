package software.amazon.adot.testbed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
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
    private Path logDirectory;
    private GenericContainer<?> collector;

    private GenericContainer<?> createAndStartCollector(String configFilePath, String logStreamName) throws IOException {

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
        try {
            logDirectory = Files.createTempDirectory("tempLogs");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory", e);
        }
        var collector = new GenericContainer<>(TEST_IMAGE)
            .withCopyFileToContainer(MountableFile.forClasspathResource(configFilePath), "/etc/collector/config.yaml")
            .withLogConsumer(new Slf4jLogConsumer(collectorLogger))
            .waitingFor(Wait.forLogMessage(".*Everything is ready. Begin running and processing data.*", 1))
            .withEnv(envVariables)
            .withClasspathResourceMapping("/logs", "/logs", BindMode.READ_WRITE)
            .withCommand("--config", "/etc/collector/config.yaml", "--feature-gates=+adot.filelog.receiver,+adot.awscloudwatchlogs.exporter,+adot.file_storage.extension");

       //Mount the Temp directory
        collector.withFileSystemBind(logDirectory.toString(),"/tempLogs", BindMode.READ_WRITE);

        collector.start();
        return collector;
    }

    @Test
    void testSyslog() throws Exception {
        String logStreamName = "rfcsyslog-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-rfcsyslog.yaml", logStreamName);

        List<String> logFilePaths = new ArrayList<>();
        logFilePaths.add("/logs/RFC5424.log");
        validateLogs(logStreamName , logFilePaths);
        collector.stop();
    }

    @Test
    void testLog4j() throws Exception {
        String logStreamName = "log4j-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-log4j.yaml", logStreamName);

        List<String> logFilePaths = new ArrayList<>();
        logFilePaths.add("/logs/log4j.log");
        validateLogs(logStreamName , logFilePaths);
        collector.stop();
    }

    @Test
    void testJson() throws Exception {
        String logStreamName = "json-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-json.yaml", logStreamName);

        List<String> logFilePaths = new ArrayList<>();
        logFilePaths.add("/logs/testingJSON.log");
        validateLogs(logStreamName , logFilePaths);
        collector.stop();
    }

    @Test
    void testCollectorRestartStorageExtension() throws Exception {
        String logStreamName = "storageExtension-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-storageExtension.yaml", logStreamName);
        File tempFile = new File(logDirectory.toString(), "storageExtension.log");
        Thread.sleep(5000);

        FileWriter fileWriter = new FileWriter(tempFile);
        try {
            fileWriter.write("First Message, collector is running" + "\n");
            fileWriter.write("Second Message, collector is running" + "\n");
            fileWriter.write("Third Message,  collector is running" + "\n");
        } catch (IOException e) {
            throw new RuntimeException("Error writing to File A.", e);
        }
        fileWriter.flush();

        List<String> logFilePaths = new ArrayList<>();
        String expectedLogPath = logDirectory.toString();
        logFilePaths.add(expectedLogPath + "/storageExtension.log");

        validateLogs(logStreamName , logFilePaths);

        collector.stop();

        Thread.sleep(5000);

        // write to the file when collector is stopped
        try {
            fileWriter.write("First Message after collector is stopped" + "\n");
            fileWriter.write("Second Message after the collector is stopped" + "\n");
            fileWriter.write("Third Message after the collector is stopped" + "\n");
            fileWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error writing to File A.", e);
        }
        fileWriter.close();
       //restarting the collector
        collector.start();

        validateLogs(logStreamName , logFilePaths);
        collector.stop();
    }

    @Test
    void testFileRotation() throws Exception {
        String logStreamName = "fileRotation-logstream-" + uniqueID;
        collector = createAndStartCollector("/configurations/config-fileRotation.yaml", logStreamName);

        Thread.sleep(5000);

        // Create and write data to File A
        File tempFile = new File(logDirectory.toString(), "testlogA.log");
        try (FileWriter fileWriter = new FileWriter(tempFile)) {
            fileWriter.write("Message in File A" + "\n");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("Error writing to File A.", e);
        }
        Thread.sleep(5000);

        File renameFile = new File(logDirectory.toString(), "testlogA-1234.log");
        tempFile.renameTo(renameFile);

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
        Thread.sleep(5000);

        List<String> logFilePaths = new ArrayList<>();
        String expectedLogPath = logDirectory.toString();
        logFilePaths.add(expectedLogPath + "/testlogA-1234.log");
        logFilePaths.add(expectedLogPath + "/testlogA.log");

        validateLogs(logStreamName, logFilePaths);

        collector.stop();
    }


    void validateLogs(String testLogStreamName, List<String> logFilePaths) throws Exception {
        var lines = new HashSet<String>();

        for (String logFilePath : logFilePaths) {
            InputStream inputStream;
            //Check whether the filePath is from resource folder.
            if (getClass().getResource(logFilePath) != null) {
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

                //Print
                System.out.println("Actual Logs - Log lines From Cloudwatch");
                messageToValidate.forEach(System.out::println);

                System.out.println("Expected logs- Log lines from log file");
                lines.forEach(System.out::println);

                //Validate body field in JSON-messageToValidate with actual log line from the log file.
                assertThat(messageToValidate.containsAll(lines)).isTrue();
                assertThat(messageToValidate).containsExactlyInAnyOrderElementsOf(lines);
                return null;
            });
    }

}
