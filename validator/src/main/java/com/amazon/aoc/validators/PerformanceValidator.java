package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.PerformanceResult;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.Statistic;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Arrays;

@Log4j2
public class PerformanceValidator implements IValidator {
  private static String outputFileName = "performance.json";
  private static int MAX_RETRY_COUNT = 30;
  private Context context;
  private ValidationConfig validationConfig;

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedDataTemplate)
      throws Exception {
    this.context = context;
    this.validationConfig = validationConfig;
  }

  // Create dimension given name and value
  private Dimension createDimension(String name, String value) {
    Dimension dimension = new Dimension();
    dimension.setName(name);
    dimension.setValue(value);
    return dimension;
  }

  // Get average stat over all datapoints
  private Double getAverageStats(List<Datapoint> datapoints) {
    Double sum = 0.0;
    if (datapoints == null || datapoints.isEmpty()) {
      return sum;
    }

    for (Datapoint dp : datapoints) {
      sum += dp.getAverage();
    }

    return sum / datapoints.size();
  }

  @Override
  public void validate() throws Exception {
    final Date endTime = new Date();
    // Convert collection duration from minutes to milliseconds
    final Integer durationMs = validationConfig.getCollectionPeriod() * 60000;
    final Date startTime = new Date(System.currentTimeMillis() - durationMs);

    final String dataRateKey = validationConfig.getDataType() + "-"
        + validationConfig.getDataRate();
    List<Dimension> dimensions = Arrays.asList(
        createDimension("testcase", validationConfig.getTestcase()),
        createDimension("commit_id", validationConfig.getCommitId()),
        createDimension("data_rate", dataRateKey),
        createDimension("InstanceId", validationConfig.getInstanceId()),
        createDimension("instance_type", validationConfig.getInstanceType()),
        createDimension("launch_date", validationConfig.getLaunchDate()),
        createDimension("exe", validationConfig.getExe()),
        createDimension("process_name", validationConfig.getProcessName()),
        createDimension("testing_ami", validationConfig.getTestingAmi()),
        createDimension("negative_soaking", validationConfig.getNegativeSoaking())
    );

    // Create requests
    final GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
        .withNamespace(context.getMetricNamespace())
        .withPeriod(validationConfig.getDatapointPeriod())
        .withStartTime(startTime)
        .withEndTime(endTime)
        .withDimensions(dimensions)
        .withStatistics(Statistic.Average);
    final GetMetricStatisticsRequest cpuStatsRequest = request
        .clone()
        .withMetricName(validationConfig.getCpuMetricName());
    final GetMetricStatisticsRequest memoryStatsRequest = request
        .clone()
        .withMetricName(validationConfig.getMemoryMetricName());

    CloudWatchService cloudWatchService = new CloudWatchService(context.getRegion());
    RetryHelper.retry(
        MAX_RETRY_COUNT,
        () -> {
          log.info("retrieving cpu statistics");
          List<Datapoint> cpuDatapoints = cloudWatchService.getDatapoints(cpuStatsRequest);
          Double avgCpu = getAverageStats(cpuDatapoints);
          log.info("retrieving memory statistics");
          List<Datapoint> memoryDatapoints = cloudWatchService.getDatapoints(memoryStatsRequest);
          Double avgMemory = getAverageStats(memoryDatapoints) / 1000000;

          final PerformanceResult result = new PerformanceResult(
              validationConfig.getTestcase(),
              validationConfig.getInstanceType(),
              validationConfig.getTestingAmi(),
              validationConfig.getDataType(),
              validationConfig.getDataRate(),
              avgCpu,
              avgMemory,
              validationConfig.getCommitId(),
              validationConfig.getCollectionPeriod()
          );

          try {
            new ObjectMapper().writeValue(new File("/var/output/" + outputFileName), result);
            log.info("Result written to " + outputFileName);
          } catch (Exception e) {
            log.error("failed to write performance result to file." + e.getMessage());
          }
        });
  }
}
