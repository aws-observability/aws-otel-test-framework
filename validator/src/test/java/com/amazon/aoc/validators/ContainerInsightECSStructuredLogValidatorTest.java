package com.amazon.aoc.validators;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.aoc.fileconfigs.PredefinedExpectedTemplate;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.logs.model.FilteredLogEvent;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class ContainerInsightECSStructuredLogValidatorTest {

  String clusterName = "fakedClusterName";
  private static final String PATH =
      "/src/test/java/com/amazon/aoc/validators/ecsinstancelogtemplate/";


  @Test
  public void testFetchAndValidateLogs() throws Exception {

    // fake a validation config
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setExpectedMetricTemplate(
        PredefinedExpectedTemplate.CONTAINER_INSIGHT_ECS_LOG.name());

    // mock cloudwatch service
    CloudWatchService cloudWatchService = mock(CloudWatchService.class);
    List<FilteredLogEvent> logEvents = getlogEvents();
    when(cloudWatchService.filterLogs(anyString(), anyString(), anyLong(), anyInt()))
        .thenReturn(logEvents);

    // go validate
    ConatinerInsightECSStructuredLogValidator validator =
        new ConatinerInsightECSStructuredLogValidator();
    validator.init(
        getContext(),
        validationConfig,
        null,
        PredefinedExpectedTemplate.CONTAINER_INSIGHT_ECS_LOG
    );
    Instant startTime = Instant.EPOCH;
    validator.setCloudWatchService(cloudWatchService);
    validator.fetchAndValidateLogs(startTime);
  }

  private Context getContext() {
    String namespace = "fakednamespace";
    String testingId = "fakedTesingId";
    String region = "us-west-2";

    // faked context
    Context context = new Context(
        testingId,
        region,
        false
    );
    context.setMetricNamespace(namespace);
    CloudWatchContext cloudWatchContext = new CloudWatchContext();
    cloudWatchContext.setClusterName(this.clusterName);

    context.setCloudWatchContext(cloudWatchContext);
    return context;
  }

  private List<FilteredLogEvent> getlogEvents() throws IOException {
    List<FilteredLogEvent> events = new ArrayList<>();
    String path = System.getProperty("user.dir")
        + PATH;
    File file = new File(path);
    File[] tempList = file.listFiles();
    for (int i = 0; i < tempList.length; i++) {
      if (tempList[i].isFile()) {
        FilteredLogEvent event = new FilteredLogEvent();
        String fileName = tempList[i].toString();
        String content = new String(Files.readAllBytes(Paths.get(fileName)));
        event.setMessage(content);
        events.add(event);
      }
    }
    return events;
  }
}
