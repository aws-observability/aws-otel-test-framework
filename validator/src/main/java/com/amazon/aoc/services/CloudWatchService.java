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

package com.amazon.aoc.services;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

import java.util.List;

/** a wrapper of cloudwatch client. */
public class CloudWatchService {
  AmazonCloudWatch amazonCloudWatch;

  /**
   * Construct CloudWatch Service with region.
   *
   * @param region the region for CloudWatch
   */
  public CloudWatchService(String region) {
    amazonCloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(region).build();
  }

  /**
   * listMetrics fetches metrics from CloudWatch.
   *
   * @param namespace the metric namespace on CloudWatch
   * @param metricName the metric name on CloudWatch
   * @return List of Metrics
   */
  public List<Metric> listMetrics(final String namespace, final String metricName) {
    final ListMetricsRequest listMetricsRequest =
        new ListMetricsRequest().withNamespace(namespace).withMetricName(metricName);
    return amazonCloudWatch.listMetrics(listMetricsRequest).getMetrics();
  }

  /**
   * putMetricData publish metric to CloudWatch.
   *
   * @param namespace the metric namespace on CloudWatch
   * @param metricName the metric name on CloudWatch
   * @param value the metric value on CloudWatch
   * @return Response of PMD call
   */
  public PutMetricDataResult putMetricData(final String namespace,
                                           final String metricName, final Double value) {
    MetricDatum datum = new MetricDatum()
            .withMetricName(metricName)
            .withUnit(StandardUnit.None)
            .withValue(value);
    PutMetricDataRequest request = new PutMetricDataRequest()
            .withNamespace(namespace)
            .withMetricData(datum);
    return amazonCloudWatch.putMetricData(request);
  }

  /**
   * getDatapoints fetches datapoints from CloudWatch using the given request.
   *
   * @param request request for datapoint
   * @return List of Datapoints
   */
  public List<Datapoint> getDatapoints(GetMetricStatisticsRequest request) {
    return amazonCloudWatch.getMetricStatistics(request).getDatapoints();
  }
}
