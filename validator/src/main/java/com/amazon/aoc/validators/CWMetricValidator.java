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
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Log4j2
public class CWMetricValidator implements IValidator {
  private static int DEFAULT_MAX_RETRY_COUNT = 30;

  private MustacheHelper mustacheHelper = new MustacheHelper();
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
    final List<Metric> expectedMetricList = cwMetricHelper.listExpectedMetrics(
        context,
        expectedMetric,
        caller
    );
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
          List<Metric> metricList =
              this.listMetricFromCloudWatch(cloudWatchService, expectedMetricList);

          // remove the skip dimensions
          log.info("dimensions to be skipped in validation: {}", skippedDimensionNameList);
          for (Metric metric : metricList) {
            metric
                .getDimensions()
                .removeIf((dimension) -> skippedDimensionNameList.contains(dimension.getName()));
          }

          log.info("check if all the expected metrics are found");
          log.info("actual metricList is {}", metricList);
          log.info("expected metricList is {}", expectedMetricList);
          compareMetricLists(expectedMetricList, metricList);

          log.info("check if there're unexpected additional metric getting fetched");
          compareMetricLists(metricList, expectedMetricList);
        });

    log.info("finish metric validation");
  }

  /**
   * Check if every metric in toBeChckedMetricList is in baseMetricList.
   *
   * @param toBeCheckedMetricList toBeCheckedMetricList
   * @param baseMetricList baseMetricList
   */
  private void compareMetricLists(List<Metric> toBeCheckedMetricList, List<Metric> baseMetricList)
      throws BaseException {

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
    for (Metric metric : baseMetricList) {
      metricSet.add(metric);
    }
    for (Metric metric : toBeCheckedMetricList) {
      if (!metricSet.contains(metric)) {
        throw new BaseException(
            ExceptionCode.EXPECTED_METRIC_NOT_FOUND,
            String.format(
                "metric in %ntoBeCheckedMetricList: %s is not found in %nbaseMetricList: %s %n",
                metric, metricSet));
      }
    }
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
    this.maxRetryCount = DEFAULT_MAX_RETRY_COUNT;
  }
}
