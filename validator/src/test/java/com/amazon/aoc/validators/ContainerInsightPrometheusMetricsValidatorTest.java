package com.amazon.aoc.validators;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.aoc.fileconfigs.PredefinedExpectedTemplate;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class ContainerInsightPrometheusMetricsValidatorTest {
  String clusterName = "fakedClusterName";
  String eksNamespace = "fakedNamespace";


  @Test
  public void testValidationSucceed() throws Exception {
    // fake a validation config
    ValidationConfig validationConfig = new ValidationConfig();
    validationConfig.setCallingType("http");
    validationConfig.setExpectedMetricTemplate(
        PredefinedExpectedTemplate.CONTAINER_INSIGHT_EKS_PROMETHEUS_METRIC.name());

    // mock cloudwatch service
    CloudWatchService cloudWatchService = mock(CloudWatchService.class);
    List<MetricDataResult> metricDataResults = new ArrayList<>();
    metricDataResults.add(new MetricDataResult().withStatusCode("200").withValues(1.0));
    when(cloudWatchService.getMetricData(any(), any(), any())).thenReturn(metricDataResults);

    // go validate
    ContainerInsightPrometheusMetricsValidator validator =
        new ContainerInsightPrometheusMetricsValidator();
    validator.init(
        getContext(),
        validationConfig,
        null,
        PredefinedExpectedTemplate.CONTAINER_INSIGHT_EKS_PROMETHEUS_METRIC
    );
    validator.setCloudWatchService(cloudWatchService);
    validator.setMaxRetryCount(1);
    validator.setInitialSleepTime(0);
    validator.validate();
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

    CloudWatchContext.App app = new CloudWatchContext.App();
    app.setJob("appjob");
    app.setName("appname");
    app.setNamespace(this.eksNamespace);
    CloudWatchContext cloudWatchContext = new CloudWatchContext();
    cloudWatchContext.setNginx(app);
    cloudWatchContext.setClusterName(this.clusterName);

    context.setCloudWatchContext(cloudWatchContext);
    return context;
  }
}
