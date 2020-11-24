package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Arrays;

@Log4j2
public class PerformanceValidator implements IValidator {
  String cpuMetricName;
  String memoryMetricName;
  String testcase;
  String commitId;
  String instanceType;
  String dataType;
  Integer dataRate;
  Integer collectionPeriod;
  Integer datapointPeriod;

  private static int MAX_RETRY_COUNT = 30;
  private static String testingType = "perf-test";
  private Context context;

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedDataTemplate)
      throws Exception {
    this.context = context;
    this.cpuMetricName = validationConfig.getCpuMetricName();
    this.memoryMetricName = validationConfig.getMemoryMetricName();
    this.testcase = validationConfig.getTestcase();
    this.commitId = validationConfig.getCommitId();
    this.instanceType = validationConfig.getInstanceType();
    this.dataType = validationConfig.getDataType();
    this.dataRate = validationConfig.getDataRate();
    this.collectionPeriod = validationConfig.getCollectionPeriod();
    this.datapointPeriod = validationConfig.getDatapointPeriod();
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

  private String buildJson(Double avgCpuStat, Double avgMemoryStat) throws JsonProcessingException {
    final PerformanceResult result = new PerformanceResult(
        this.testcase,
        this.instanceType,
        this.dataType,
        this.dataRate,
        avgCpuStat,
        avgMemoryStat,
        this.commitId,
        this.collectionPeriod
    );
    return new ObjectMapper().writeValueAsString(result);
  }

  @Override
  public void validate() throws Exception {
    final Date endTime = new Date();
    // Convert collection duration from minutes to milliseconds
    final Integer durationMs = this.collectionPeriod * 60000;
    final Date startTime = new Date(System.currentTimeMillis() - durationMs);

    List<Dimension> dimensions = Arrays.asList(
        createDimension("testcase", this.testcase),
        createDimension("commit_id", this.commitId),
        createDimension("data_rate", this.dataType + "-" + this.dataRate),
        createDimension("instance_type", this.instanceType),
        createDimension("testing_type", testingType)
    );

    // Create requests
    final GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
        .withNamespace(context.getMetricNamespace())
        .withPeriod(this.datapointPeriod)
        .withStartTime(startTime)
        .withEndTime(endTime)
        .withDimensions(dimensions)
        .withStatistics(Statistic.Average);
    final GetMetricStatisticsRequest cpuStatsRequest = request
        .withMetricName(this.cpuMetricName);
    final GetMetricStatisticsRequest memoryStatsRequest = request
        .withMetricName(this.memoryMetricName);

    CloudWatchService cloudWatchService = new CloudWatchService(context.getRegion());
    RetryHelper.retry(
        MAX_RETRY_COUNT,
        () -> {
          log.info("retrieving cpu statistics");
          List<Datapoint> cpuDatapoints = cloudWatchService.getDatapoints(cpuStatsRequest);
          Double avgCpu = getAverageStats(cpuDatapoints);
          log.info("retrieving memory statistics");
          List<Datapoint> memoryDatapoints = cloudWatchService.getDatapoints(memoryStatsRequest);
          Double avgMemory = getAverageStats(memoryDatapoints);

          try {
            String jsonResult = buildJson(avgCpu, avgMemory);
            log.info(jsonResult);
          } catch (Exception e) {
            log.error("failed to convert result to json." + e.getMessage());
          }
        });
  }
}
