/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amazon.aoc.models.prometheus.PrometheusMetric;
import com.amazon.aoc.services.CortexService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j2
public class PrometheusStaticMetricValidator implements IValidator {
  private static final int MAX_RETRY_COUNT = 30;

  private final MustacheHelper mustacheHelper = new MustacheHelper();
  private Context context;
  private FileConfig expectedMetric;
  private ValidationConfig validationConfig;
  private CortexService cortexService;

  public void validate() throws Exception {
    log.info("Start prometheus metric validating");

    // get expected metrics
    final List<PrometheusMetric> expectedMetricList = this.listExpectedMetrics(
      context,
      expectedMetric,
      caller,
      true);

    // get metrics from prometheus
    RetryHelper.retry(
        MAX_RETRY_COUNT,
        () -> {
          List<PrometheusMetric> metricList =
                  this.listMetricFromPrometheus(cortexService, expectedMetricList);

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
   * @param baseMetricList        baseMetricList
   */
  private void compareMetricLists(List<PrometheusMetric> toBeCheckedMetricList,
                                  List<PrometheusMetric> baseMetricList)
          throws BaseException {
    // load metrics into a tree set
    Comparator<PrometheusMetric> comparator = PrometheusMetric::staticPrometheusMetricCompare;

    if (validationConfig.getShouldValidateMetricValue()) {
      comparator = PrometheusMetric::staticPrometheusMetricCompare;
    }

    Set<PrometheusMetric> metricSet = new TreeSet<>(comparator);
    metricSet.addAll(baseMetricList);

    for (PrometheusMetric metric : toBeCheckedMetricList) {
      if (!metricSet.contains(metric)) {
        throw new BaseException(
                ExceptionCode.EXPECTED_METRIC_NOT_FOUND,
                String.format(
                        "metric in toBeCheckedMetricList %s not found in baseMetricList: %s",
                        metric, metricSet));
      }
    }
  }

  private List<PrometheusMetric> getExpectedMetricList(Context context) throws Exception {
    log.info("getting expected metrics");
    // get expected metrics as yaml from config
    String yamlExpectedMetrics =  mustacheHelper.render(this.expectedMetric, context);

    // load metrics from yaml
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<PrometheusMetric> expectedMetricList =
        mapper.readValue(
          yamlExpectedMetrics.getBytes(StandardCharsets.UTF_8),
            new TypeReference<List<PrometheusMetric>>() {
            });

    return removeSkippedMetrics(expectedMetricList);
  }

  private List<PrometheusMetric> removeSkippedMetrics(List<PrometheusMetric> expectedMetrics) {
    return expectedMetrics.stream()
            .filter(metric -> !metric.isSkippedMetric())
            .collect(Collectors.toList());
  }

  private List<PrometheusMetric> listMetricFromPrometheus(
          CortexService cortexService, List<PrometheusMetric> expectedMetricList)
          throws IOException, URISyntaxException, BaseException {
    // put metric name to search metric
    HashMap<String, String> metricNameSet = new HashSet<>();
    for (PrometheusMetric metric : expectedMetricList) {
      metricNameSet.add(metric.getMetricName());
    }
    // search by metric name
    List<PrometheusMetric> result = new ArrayList<>();
    for (PrometheusMetric metricName : metricNameSet) {
      result.addAll(cortexService.listMetricsLastHour(expectedMetric.getMetricName());
    }
    return removeSkippedMetrics(result);
  }

  @Override
  public void init(
          Context context,
          ValidationConfig validationConfig,
          ICaller caller,
          FileConfig expectedMetricTemplate)
          throws Exception {
    this.context = context;
    this.cortexService = new CortexService(context);
    this.expectedMetric = expectedMetricTemplate;
  }
}
