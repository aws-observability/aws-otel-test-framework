package com.amazon.aoc.validators;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.aoc.fileconfigs.PredefinedExpectedTemplate;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ContainerInsightPrometheusMetricsValidatorTest {
    String clusterName = "fakedClusterName";
    String eksNamespace = "fakedNamespace";

    @Test
    public void testValidationSucceed() throws Exception {
        // mock cloudwatch service
        CloudWatchService cloudWatchService = mock(CloudWatchService.class);
        List<MetricDataResult> metricDataResults = new ArrayList<>();
        metricDataResults.add(new MetricDataResult().withStatusCode("200").withValues(1.0));
        when(cloudWatchService.getMetricData(any(), any(), any())).thenReturn(metricDataResults);

        // go validate
        ContainerInsightPrometheusMetricsValidator validator = new ContainerInsightPrometheusMetricsValidator();
        validator.init(getContext(), null, null, PredefinedExpectedTemplate.CONTAINER_INSIGHT_EKS_PROMETHEUS_METRIC);
        validator.setCloudWatchService(cloudWatchService);
        validator.setMaxRetryCount(1);
        validator.setInitialSleepTime(0);
        validator.validate();
    }

    @Test
    public void tesECS() throws Exception {
        CloudWatchService cloudWatchService = mock(CloudWatchService.class);
        List<MetricDataResult> metricDataResults = new ArrayList<>();
        metricDataResults.add(new MetricDataResult().withStatusCode("200").withValues(1.0));
        when(cloudWatchService.getMetricData(any(), any(), any())).thenReturn(metricDataResults);

        ContainerInsightPrometheusMetricsValidator validator = new ContainerInsightPrometheusMetricsValidator();
        validator.init(getECSContext(), null, null, PredefinedExpectedTemplate.CONTAINER_INSIGHT_ECS_PROMETHEUS_METRIC);
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
        Context context = new Context(testingId, region, false, true);
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

    private Context getECSContext() {
        CloudWatchContext.App jmx = new CloudWatchContext.App();
        jmx.setJob("jmx");
        jmx.setTaskDefinitionFamilies(new String[]{"jmxawsvpc", "jmxfargate"});
        CloudWatchContext cloudWatchContext = new CloudWatchContext();
        cloudWatchContext.setJmx(jmx);
        cloudWatchContext.setClusterName(this.clusterName);

        Context context = getContext();
        context.setCloudWatchContext(cloudWatchContext);
        return context;
    }
}
