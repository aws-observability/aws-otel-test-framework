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
import com.amazon.aoc.helpers.CWMetricHelper;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2
public class CWMetricValidator implements IValidator {
  private ICaller caller;
  private Context context;
  private FileConfig expectedMetric;

  private CloudWatchService cloudWatchService;
  private CWMetricHelper cwMetricHelper;
  private int maxRetryCount;

  // for unit test
  public void setCloudWatchService(CloudWatchService cloudWatchService) {
    this.cloudWatchService = cloudWatchService;
  }

  // for unit test so that we lower the count to 1
  public void setMaxRetryCount(int maxRetryCount) {
    this.maxRetryCount = maxRetryCount;
  }

  @Override
  public void validate() throws Exception {
    log.info("Start metric validating");
    // get expected metrics and remove the to be skipped dimensions
    final List<Metric> expectedMetricList =
        cwMetricHelper.listExpectedMetrics(context, expectedMetric, caller);
    Set<String> skippedDimensionNameList = new HashSet<>();
    for (Metric metric : expectedMetricList) {
      for (Dimension dimension : metric.getDimensions()) {

        if (dimension.getValue() == null || dimension.getValue().equals("")) {
          continue;
        }

        if (dimension.getValue().equals("SKIP")) {
          skippedDimensionNameList.add(dimension.getName());
        }
      }
    }
    for (Metric metric : expectedMetricList) {
      metric
          .getDimensions()
          .removeIf((dimension) -> skippedDimensionNameList.contains(dimension.getName()));
    }

    // get metric from cloudwatch
    RetryHelper.retry(
        maxRetryCount,
        () -> {
          List<Metric> actualMetricList =
              this.listMetricFromCloudWatch(cloudWatchService, expectedMetricList);

          // remove the skip dimensions
          log.info("dimensions to be skipped in validation: {}", skippedDimensionNameList);
          for (Metric metric : actualMetricList) {
            metric
                .getDimensions()
                .removeIf((dimension) -> skippedDimensionNameList.contains(dimension.getName()));
          }

          log.info("check if all the expected metrics are found");
          log.info("actual metricList is {}", actualMetricList);
          log.info("expected metricList is {}", expectedMetricList);

          compareMetricLists(expectedMetricList, actualMetricList);
        });

    log.info("finish metric validation");
  }

  /**
   * Check if every metric in expectedMetricList is in actualMetricList. This performs an exact
   * match between two metric lists. This does not allow any extra metrics + dimension set
   * combinations to be present in the actual metric list.
   *
   * @param expectedMetricList The list of expected metrics
   * @param actualMetricList The list of actual metrics retrieved from CloudWatch
   */
  private void compareMetricLists(List<Metric> expectedMetricList, List<Metric> actualMetricList)
      throws BaseException {

    // check if all expected values are present
    Set<Metric> actualMetricSet = buildMetricSet(actualMetricList);
    for (Metric metric : expectedMetricList) {
      if (!actualMetricSet.contains(metric)) {
        throw new BaseException(
            ExceptionCode.EXPECTED_METRIC_NOT_FOUND,
            String.format("expected metric %s is not found in actual metric list %n", metric));
      }
    }

    // check if any additional metric and dimension set combinations are present in actual metric
    // list
    Set<Metric> expectedMetricSet = buildMetricSet(expectedMetricList);
    for (Metric metric : actualMetricList) {
      if (!expectedMetricSet.contains(metric)) {
        throw new BaseException(
            ExceptionCode.UNEXPECTED_METRIC_FOUND,
            String.format("unexpected metric %s found in actual metric list %n", metric));
      }
    }
  }

  @NotNull private static Set<Metric> buildMetricSet(List<Metric> inputMetricList) {
    // load metrics into a hash set
    Set<Metric> metricSet =
        new TreeSet<>(
            (Metric o1, Metric o2) -> {
              // check namespace
              if (!o1.getNamespace().equals(o2.getNamespace())) {
                return o1.getNamespace().compareTo(o2.getNamespace());
              }

              // check metric name
              if (!o1.getMetricName().equals(o2.getMetricName())) {
                return o1.getMetricName().compareTo(o2.getMetricName());
              }

              // sort and check dimensions
              List<Dimension> dimensionList1 = o1.getDimensions();
              List<Dimension> dimensionList2 = o2.getDimensions();

              // sort
              dimensionList1.sort(Comparator.comparing(Dimension::getName));
              dimensionList2.sort(Comparator.comparing(Dimension::getName));

              return dimensionList1.toString().compareTo(dimensionList2.toString());
            });
    for (Metric metric : inputMetricList) {
      metricSet.add(metric);
    }
    return metricSet;
  }

  private List<Metric> listMetricFromCloudWatch(
      CloudWatchService cloudWatchService, List<Metric> expectedMetricList) throws IOException {
    // put namespace into the map key, so that we can use it to search metric
    HashMap<String, String> metricNameMap = new HashMap<>();
    for (Metric metric : expectedMetricList) {
      metricNameMap.put(metric.getMetricName(), metric.getNamespace());
    }

    // search by metric name
    List<Metric> result = new ArrayList<>();
    for (String metricName : metricNameMap.keySet()) {
      result.addAll(cloudWatchService.listMetrics(metricNameMap.get(metricName), metricName));
    }
    return result;
  }

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedMetricTemplate)
      throws Exception {
    this.context = context;
    this.caller = caller;
    this.expectedMetric = expectedMetricTemplate;
    this.cloudWatchService = new CloudWatchService(context.getRegion());
    this.cwMetricHelper = new CWMetricHelper();
    int DEFAULT_MAX_RETRY_COUNT = 30;
    this.maxRetryCount = DEFAULT_MAX_RETRY_COUNT;
  }
}
