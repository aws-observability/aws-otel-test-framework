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
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricDataResult;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDataQuery;
import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.MetricStat;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.FilterLogEventsRequest;
import com.amazonaws.services.logs.model.FilterLogEventsResult;
import com.amazonaws.services.logs.model.FilteredLogEvent;
import com.amazonaws.services.logs.model.GetLogEventsRequest;
import com.amazonaws.services.logs.model.GetLogEventsResult;
import com.amazonaws.services.logs.model.OutputLogEvent;
import java.util.Date;
import java.util.List;
import lombok.extern.log4j.Log4j2;

/**
 * a wrapper of cloudwatch client.
 */
@Log4j2
public class CloudWatchService {
    private static final int MAX_QUERY_PERIOD = 60;
    private static final String REQUESTER = "integrationTest";

    private AmazonCloudWatch amazonCloudWatch;
    private AWSLogs awsLogs;

    /**
     * Construct CloudWatch Service with region.
     *
     * @param region
     *            the region for CloudWatch
     */
    public CloudWatchService(String region) {
        amazonCloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(region).build();
        awsLogs = AWSLogsClientBuilder.standard().withRegion(region).build();
    }

    /**
     * listMetrics fetches metrics from CloudWatch.
     *
     * @param namespace
     *            the metric namespace on CloudWatch
     * @param metricName
     *            the metric name on CloudWatch
     * @return List of Metrics
     */
    public List<Metric> listMetrics(final String namespace, final String metricName) {
        final ListMetricsRequest listMetricsRequest = new ListMetricsRequest().withNamespace(namespace)
                .withMetricName(metricName);
        return amazonCloudWatch.listMetrics(listMetricsRequest).getMetrics();
    }

    /**
     * getMetricData fetches the history data of the given metric from CloudWatch.
     * 
     * @param metric
     *            metric query object
     * @param startTime
     *            the start timestamp
     * @param endTime
     *            the end timestamp
     * @return List of MetricDataResult
     */
    public List<MetricDataResult> getMetricData(Metric metric, Date startTime, Date endTime) {
        MetricStat stat = new MetricStat().withMetric(metric).withStat("Average").withPeriod(MAX_QUERY_PERIOD);
        MetricDataQuery query = new MetricDataQuery().withMetricStat(stat).withId(REQUESTER);
        final GetMetricDataRequest request = new GetMetricDataRequest().withMetricDataQueries(query)
                .withStartTime(startTime).withEndTime(endTime);

        GetMetricDataResult result = amazonCloudWatch.getMetricData(request);
        return result.getMetricDataResults();
    }

    /**
     * putMetricData publish metric to CloudWatch.
     *
     * @param namespace
     *            the metric namespace on CloudWatch
     * @param metricName
     *            the metric name on CloudWatch
     * @param value
     *            the metric value on CloudWatch
     * @param dimensions
     *            the dimensions of metric
     * @return Response of PMD call
     */
    public PutMetricDataResult putMetricData(final String namespace, final String metricName, final Double value,
            final Dimension... dimensions) {
        MetricDatum datum = new MetricDatum().withMetricName(metricName).withUnit(StandardUnit.None)
                .withDimensions(dimensions).withValue(value);
        PutMetricDataRequest request = new PutMetricDataRequest().withNamespace(namespace).withMetricData(datum);
        return amazonCloudWatch.putMetricData(request);
    }

    /**
     * getDatapoints fetches datapoints from CloudWatch using the given request.
     *
     * @param request
     *            request for datapoint
     * @return List of Datapoints
     */
    public List<Datapoint> getDatapoints(GetMetricStatisticsRequest request) {
        return amazonCloudWatch.getMetricStatistics(request).getDatapoints();
    }

    /**
     * getLogs fetches log entries from CloudWatch.
     * 
     * @param logGroupName
     *            the log group name
     * @param logStreamName
     *            the log stream name
     * @param startFromTimestamp
     *            the start timestamp
     * @param limit
     *            the maximum number of log events to be returned in a single query
     * @return List of OutputLogEvent
     */
    public List<OutputLogEvent> getLogs(String logGroupName, String logStreamName, long startFromTimestamp, int limit) {
        GetLogEventsRequest request = new GetLogEventsRequest().withLogGroupName(logGroupName)
                .withLogStreamName(logStreamName).withStartTime(startFromTimestamp).withLimit(limit);

        GetLogEventsResult result = awsLogs.getLogEvents(request);
        return result.getEvents();
    }

    /**
     * filterLogs filters log entries from CloudWatch.
     * 
     * @param logGroupName
     *            the log group name
     * @param filterPattern
     *            the filter pattern, see
     *            https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/FilterAndPatternSyntax.html
     * @param startFromTimestamp
     *            the start timestamp
     * @param limit
     *            the maximum number of log events to be returned in a single query
     * @return List of FilteredLogEvent
     */
    public List<FilteredLogEvent> filterLogs(String logGroupName, String filterPattern, long startFromTimestamp,
            int limit) {
        FilterLogEventsRequest request = new FilterLogEventsRequest().withLogGroupName(logGroupName)
                .withStartTime(startFromTimestamp).withFilterPattern(filterPattern).withLimit(limit);

        FilterLogEventsResult result = awsLogs.filterLogEvents(request);
        return result.getEvents();
    }
}
