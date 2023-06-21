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

import com.amazon.aoc.clients.CortexClient;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.prometheus.PrometheusMetric;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.List;

/**
 * a wrapper around the prometheus client.
 */
public class CortexService {
    private static final int INTERVAL = 20;

    private final Context context;

    /**
     * Construct Prometheus Service with region.
     */
    public CortexService(Context context) {
        this.context = context;
    }

    /**
     * listMetricsFromSampleApp fetches metrics from cortex instance using the
     * timestamp provided from the sample app. Note that we add an interval to the
     * timestamp to ensure that Prometheus has had time to scrape the metric.
     *
     * @param query
     *            the Prometheus expression query string
     * @param timestamp
     *            the timestamp from sample app
     * @return List of PrometheusMetrics
     */
    public List<PrometheusMetric> listMetricsWithTimestamp(final String query, final String timestamp)
            throws IOException, URISyntaxException, BaseException {
        BigDecimal newTimestamp = new BigDecimal(timestamp).add(new BigDecimal(INTERVAL));
        return listMetrics(query, newTimestamp.toPlainString());
    }

    /**
     * listMetricsLastHour fetches metrics from the last hour from cortex instance.
     *
     * @param query
     *            the Prometheus expression query string
     * @return List of PrometheusMetrics
     */
    public List<PrometheusMetric> listMetricsLastHour(final String query)
            throws IOException, URISyntaxException, BaseException {
        return new CortexClient(context).queryMetricLastHour(query);
    }

    /**
     * listMetrics fetches metrics from cortex instance.
     *
     * @param query
     *            the Prometheus expression query string
     * @param timestamp
     *            the evaluation timestamp
     * @return List of PrometheusMetrics
     */
    public List<PrometheusMetric> listMetrics(final String query, final String timestamp)
            throws IOException, URISyntaxException, BaseException {
        return new CortexClient(context).query(query, timestamp);
    }
}
