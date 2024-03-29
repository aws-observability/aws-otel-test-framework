/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import com.amazonaws.util.StringUtils;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;

/**
 * AbstractMetricsValidator is diverged from CWMetricValidator for container insight metrics
 * validation. In this validator, we exclude specific dimension checking, e.g. OTelLib and metrics
 * rolling up.
 */
// TODO: We hope to merge it with CWMetricValidator when the validation process
// is finalized.
@Log4j2
public abstract class AbstractCWMetricsValidator implements IValidator {
  private CloudWatchService cloudWatchService;
  private List<Metric> expectedMetrics;

  private static final int DEFAULT_MAX_RETRY_COUNT = 10;
  private static final int DEFAULT_INITIAL_SLEEP_TIME = 60;
  private static final int CHECK_INTERVAL_IN_MILLI = 30 * 1000;
  private static final int CHECK_DURATION_IN_SECONDS = 2 * 60;

  private int maxRetryCount;
  private int initialSleepTime;

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedDataTemplate)
      throws Exception {
    cloudWatchService = new CloudWatchService(context.getRegion());
    expectedMetrics = getExpectedMetrics(context, expectedDataTemplate);
    this.maxRetryCount = DEFAULT_MAX_RETRY_COUNT;
    this.initialSleepTime = DEFAULT_INITIAL_SLEEP_TIME;
  }

  abstract List<Metric> getExpectedMetrics(Context context, FileConfig expectedDataTemplate)
      throws Exception;

  @Override
  public void validate() throws Exception {
    log.info("[ContainerInsight] start validating metrics, pause 60s for metric collection");
    TimeUnit.SECONDS.sleep(initialSleepTime);
    log.info("[ContainerInsight] resume validation");

    RetryHelper.retry(
        maxRetryCount,
        CHECK_INTERVAL_IN_MILLI,
        true,
        () -> {
          Instant startTime =
              Instant.now().minusSeconds(CHECK_DURATION_IN_SECONDS).truncatedTo(ChronoUnit.MINUTES);
          Instant endTime = startTime.plusSeconds(CHECK_DURATION_IN_SECONDS);

          for (Metric expectedMetric : expectedMetrics) {
            List<MetricDataResult> batchResult =
                cloudWatchService.getMetricData(
                    expectedMetric, Date.from(startTime), Date.from(endTime));
            boolean found = false;
            for (MetricDataResult result : batchResult) {
              if (result.getValues().size() > 0) {
                found = true;
              }
            }
            if (!found) {
              throw new BaseException(
                  ExceptionCode.EXPECTED_METRIC_NOT_FOUND,
                  String.format(
                      "[ContainerInsight] metric %s not found with dimension %s",
                      expectedMetric.getMetricName(), stringifyDimension(expectedMetric)));
            }
          }
        });
    log.info("[ContainerInsight] finish validation successfully");
  }

  private static String stringifyDimension(Metric metric) {
    if (metric.getDimensions() == null) {
      return "[]";
    }
    String[] dimensions = new String[metric.getDimensions().size()];
    for (int i = 0; i < metric.getDimensions().size(); i++) {
      Dimension dimension = metric.getDimensions().get(i);
      dimensions[i] = String.format("%s: %s", dimension.getName(), dimension.getValue());
    }
    return "[" + StringUtils.join(", ", dimensions) + "]";
  }

  public void setCloudWatchService(CloudWatchService cloudWatchService) {
    this.cloudWatchService = cloudWatchService;
  }

  public void setMaxRetryCount(int maxRetryCount) {
    this.maxRetryCount = maxRetryCount;
  }

  public void setInitialSleepTime(int initialSleepTime) {
    this.initialSleepTime = initialSleepTime;
  }
}
