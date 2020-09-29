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

import com.amazon.aoc.enums.GenericConstants;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Log4j2
public class MetricValidator implements IValidator {
  private static int MAX_RETRY_COUNT = 60;
  private static final String DEFAULT_DIMENSION_NAME = "OTLib";
  private static final String DEFAULT_DIMENSION_VALUE = "cloudwatch-otel";

  private MustacheHelper mustacheHelper = new MustacheHelper();
  private Context context;

  @Override
  public void validate() throws Exception {
    log.info("Start metric validating");
    final List<Metric> expectedMetricList = this.getExpectedMetricList(context);
    CloudWatchService cloudWatchService =
        new CloudWatchService(context.getRegion());

    RetryHelper.retry(
        MAX_RETRY_COUNT,
        () -> {
          List<Metric> metricList =
              this.listMetricFromCloudWatch(cloudWatchService, expectedMetricList);
          log.info("check if all the expected metrics are found");
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
    log.info("compare two metric list {} {}", toBeCheckedMetricList, baseMetricList);
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
                "metric in toBeCheckedMetricList %s not found in baseMetricList: %s",
                metric, metricSet));
      }
    }
  }

  private List<Metric> getExpectedMetricList(Context context) throws IOException {
    // get expected metrics as yaml from config
    String yamlExpectedMetrics = mustacheHelper.render(context.getExpectedMetric(), context);

    // load metrics from yaml
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<Metric> expectedMetricList =
        mapper.readValue(
            yamlExpectedMetrics.getBytes(StandardCharsets.UTF_8),
            new TypeReference<List<Metric>>() {});

    return rollupMetric(expectedMetricList);
  }

  private List<Metric> listMetricFromCloudWatch(
      CloudWatchService cloudWatchService, List<Metric> expectedMetricList) throws IOException {
    Set<String> metricNameSet = new HashSet();
    for (Metric metric : expectedMetricList) {
      metricNameSet.add(metric.getMetricName());
    }

    // search by metric name
    List<Metric> result = new ArrayList<>();
    for (String metricName : metricNameSet) {
      result.addAll(
          cloudWatchService.listMetrics(
            context.getNamespace(),
            metricName));
    }

    return result;
  }

  private List<Metric> rollupMetric(List<Metric> metricList) {
    List<Metric> rollupMetricList = new ArrayList<>();
    for (Metric metric : metricList) {
      // all dimension rollup
      Metric allDimensionsMetric = new Metric();
      allDimensionsMetric.setMetricName(metric.getMetricName());
      allDimensionsMetric.setNamespace(metric.getNamespace());
      allDimensionsMetric.setDimensions(metric.getDimensions());
      allDimensionsMetric
          .getDimensions()
          .add(new Dimension().withName(DEFAULT_DIMENSION_NAME).withValue(DEFAULT_DIMENSION_VALUE));
      rollupMetricList.add(allDimensionsMetric);

      // zero dimension rollup
      Metric zeroDimensionMetric = new Metric();
      zeroDimensionMetric.setNamespace(metric.getNamespace());
      zeroDimensionMetric.setMetricName(metric.getMetricName());
      zeroDimensionMetric.setDimensions(
          Arrays.asList(
              new Dimension().withName(DEFAULT_DIMENSION_NAME).withValue(DEFAULT_DIMENSION_VALUE)));
      rollupMetricList.add(zeroDimensionMetric);

      // single dimension rollup
      for (Dimension dimension : metric.getDimensions()) {
        Metric singleDimensionMetric = new Metric();
        singleDimensionMetric.setNamespace(metric.getNamespace());
        singleDimensionMetric.setMetricName(metric.getMetricName());
        singleDimensionMetric.setDimensions(
            Arrays.asList(
                new Dimension()
                    .withName(DEFAULT_DIMENSION_NAME)
                    .withValue(DEFAULT_DIMENSION_VALUE)));
        singleDimensionMetric.getDimensions().add(dimension);
        rollupMetricList.add(singleDimensionMetric);
      }
    }

    return rollupMetricList;
  }

  private String metricToString(Metric metric) {
    StringBuffer strMetric = new StringBuffer(metric.getMetricName() + "/");
    for (Dimension dimension : metric.getDimensions()) {
      strMetric.append(dimension.getName() + ":" + dimension.getValue());
      strMetric.append(",");
    }
    return strMetric.toString();
  }

  @Override
  public void init(Context context) throws Exception {
    this.context = context;
  }
}
