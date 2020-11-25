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
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Log4j2
public class CWMetricValidator implements IValidator {
  private static int MAX_RETRY_COUNT = 30;
  private static final String DEFAULT_DIMENSION_NAME = "OTelLib";

  private MustacheHelper mustacheHelper = new MustacheHelper();
  private ICaller caller;
  private Context context;
  private FileConfig expectedMetric;

  @Override
  public void validate() throws Exception {
    log.info("Start metric validating");
    // get expected metrics and remove the to be skipped dimensions
    final List<Metric> expectedMetricList = this.getExpectedMetricList(context);
    Set<String> skippedDimensionNameList = new HashSet<>();
    for (Metric metric : expectedMetricList) {
      for (Dimension dimension : metric.getDimensions()) {
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
    CloudWatchService cloudWatchService = new CloudWatchService(context.getRegion());
    RetryHelper.retry(
        MAX_RETRY_COUNT,
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
                "metric in toBeCheckedMetricList %s not found in baseMetricList: %s",
                metric, metricSet));
      }
    }
  }

  private List<Metric> getExpectedMetricList(Context context) throws Exception {
    // call endpoint
    if (caller != null) {
      caller.callSampleApp();
    }

    // get expected metrics as yaml from config
    String yamlExpectedMetrics = mustacheHelper.render(this.expectedMetric, context);

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

  private List<Metric> rollupMetric(List<Metric> metricList) {
    List<Metric> rollupMetricList = new ArrayList<>();
    for (Metric metric : metricList) {
      Dimension otellibDimension = new Dimension();
      boolean otelLibDimensionExisted = false;

      if (metric.getDimensions().size() > 0) {
        // get otellib dimension out
        // assuming the first dimension is otellib, if not the validation fails
        otellibDimension = metric.getDimensions().get(0);
        otelLibDimensionExisted = otellibDimension.getName().equals(DEFAULT_DIMENSION_NAME);
      }
      
      if (otelLibDimensionExisted) {
        metric.getDimensions().remove(0);
      }

      // all dimension rollup
      Metric allDimensionsMetric = new Metric();
      allDimensionsMetric.setMetricName(metric.getMetricName());
      allDimensionsMetric.setNamespace(metric.getNamespace());
      allDimensionsMetric.setDimensions(metric.getDimensions());

      if (otelLibDimensionExisted) {
        allDimensionsMetric
            .getDimensions()
            .add(
                new Dimension()
                    .withName(otellibDimension.getName())
                    .withValue(otellibDimension.getValue()));
      }
      rollupMetricList.add(allDimensionsMetric);

      // zero dimension rollup
      Metric zeroDimensionMetric = new Metric();
      zeroDimensionMetric.setNamespace(metric.getNamespace());
      zeroDimensionMetric.setMetricName(metric.getMetricName());

      if (otelLibDimensionExisted) {
        zeroDimensionMetric.setDimensions(
            Arrays.asList(
                new Dimension()
                    .withName(otellibDimension.getName())
                    .withValue(otellibDimension.getValue())));
      }
      rollupMetricList.add(zeroDimensionMetric);

      // single dimension rollup
      for (Dimension dimension : metric.getDimensions()) {
        Metric singleDimensionMetric = new Metric();
        singleDimensionMetric.setNamespace(metric.getNamespace());
        singleDimensionMetric.setMetricName(metric.getMetricName());
        if (otelLibDimensionExisted) {
          singleDimensionMetric.setDimensions(
              Arrays.asList(
                  new Dimension()
                      .withName(otellibDimension.getName())
                      .withValue(otellibDimension.getValue())));
        }
        singleDimensionMetric.getDimensions().add(dimension);
        rollupMetricList.add(singleDimensionMetric);
      }
    }

    return rollupMetricList;
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
  }
}
