package main

import (
	"fmt"
	"strings"

	"github.com/prometheus/client_golang/prometheus"
	dto "github.com/prometheus/client_model/go"
)

type metricCollector struct {
	counters   []prometheus.Counter
	gauges     []prometheus.Gauge
	histograms []prometheus.Histogram
	summarys   []prometheus.Summary

	metricCount int
	timestamp   float64
}

func newMetricCollector() metricCollector {
	return metricCollector{}
}

func (mc metricCollector) convertMetricsToExportedMetrics() *[]MetricResponse {
	metricsResponse := []MetricResponse{}

	//mc.handleCounters(&metricsResponse)
	mc.handleGauges(&metricsResponse)
	mc.handleHistograms(&metricsResponse)
	mc.handleSummarys(&metricsResponse)

	return &metricsResponse
}

// Prometheus Receiver failed to handle counter type of metrics for some reason.
// Disable it in Prometheus test sample app for now. Will need to follow up with AMP team on real root causes.
/*
func (mc metricCollector) handleCounters(metricsResponse *[]MetricResponse) {
	for _, counter := range mc.counters {
		metric := &dto.Metric{}
		counter.Write(metric)
		labels := convertLabelPairsToLabels(metric.GetLabel())
		labels["__name__"] = getName(counter) + "_total"
		values := convertMetricValues(mc.timestamp, metric.GetCounter().GetValue())

		*metricsResponse = append(*metricsResponse, MetricResponse{
			Labels: labels,
			Value:  values,
		})
	}
}
*/

func (mc metricCollector) handleGauges(metricsResponse *[]MetricResponse) {
	for _, gauge := range mc.gauges {
		metric := &dto.Metric{}
		gauge.Write(metric)

		labels := convertLabelPairsToLabels(metric.GetLabel())
		labels["__name__"] = getName(gauge)
		values := convertMetricValues(mc.timestamp, metric.GetGauge().GetValue())

		*metricsResponse = append(*metricsResponse, MetricResponse{
			Labels: labels,
			Value:  values,
		})
	}
}

func (mc metricCollector) handleHistograms(metricsResponse *[]MetricResponse) {
	for _, histogram := range mc.histograms {
		metric := &dto.Metric{}
		histogram.Write(metric)

		// handle count
		countLabels := convertLabelPairsToLabels(metric.GetLabel())
		countLabels["__name__"] = getName(histogram) + "_count"
		countValues := convertMetricValues(mc.timestamp, float64(metric.GetHistogram().GetSampleCount()))

		*metricsResponse = append(*metricsResponse, MetricResponse{
			Labels: countLabels,
			Value:  countValues,
		})

		// handle sum
		sumLabels := convertLabelPairsToLabels(metric.GetLabel())
		sumLabels["__name__"] = getName(histogram) + "_sum"
		sumValues := convertMetricValues(mc.timestamp, metric.GetHistogram().GetSampleSum())

		*metricsResponse = append(*metricsResponse, MetricResponse{
			Labels: sumLabels,
			Value:  sumValues,
		})

		// handle buckets
		for _, bucket := range metric.GetHistogram().GetBucket() {
			labels := convertLabelPairsToLabels(metric.GetLabel())
			labels["__name__"] = getName(histogram) + "_bucket"
			labels["le"] = fmt.Sprintf("%f", bucket.GetUpperBound())
			values := convertMetricValues(mc.timestamp, float64(bucket.GetCumulativeCount()))

			*metricsResponse = append(*metricsResponse, MetricResponse{
				Labels: labels,
				Value:  values,
			})
		}
	}
}

func (mc metricCollector) handleSummarys(metricsResponse *[]MetricResponse) {
	for _, summary := range mc.summarys {
		metric := &dto.Metric{}
		summary.Write(metric)

		// handle count
		countLabels := convertLabelPairsToLabels(metric.GetLabel())
		countLabels["__name__"] = getName(summary) + "_count"
		countValues := convertMetricValues(mc.timestamp, float64(metric.GetSummary().GetSampleCount()))

		*metricsResponse = append(*metricsResponse, MetricResponse{
			Labels: countLabels,
			Value:  countValues,
		})

		// handle sum
		sumLabels := convertLabelPairsToLabels(metric.GetLabel())
		sumLabels["__name__"] = getName(summary) + "_sum"
		sumValues := convertMetricValues(mc.timestamp, metric.GetSummary().GetSampleSum())

		*metricsResponse = append(*metricsResponse, MetricResponse{
			Labels: sumLabels,
			Value:  sumValues,
		})

		// handle quantiles
		for _, quantile := range metric.GetSummary().GetQuantile() {
			labels := convertLabelPairsToLabels(metric.GetLabel())
			labels["__name__"] = getName(summary)
			labels["quantile"] = fmt.Sprintf("%f", quantile.GetQuantile())
			values := convertMetricValues(mc.timestamp, float64(quantile.GetValue()))
			*metricsResponse = append(*metricsResponse, MetricResponse{
				Labels: labels,
				Value:  values,
			})
		}
	}
}

func convertLabelPairsToLabels(labelPairs []*dto.LabelPair) map[string]string {
	labels := map[string]string{}
	for _, labelPair := range labelPairs {
		labels[labelPair.GetName()] = labelPair.GetValue()
	}
	return labels
}

func convertMetricValues(timestamp float64, value float64) []string {
	values := make([]string, 2)
	values[0] = fmt.Sprintf("%f", timestamp)
	values[1] = fmt.Sprintf("%f", value)
	return values
}

type metricType interface {
	Desc() *prometheus.Desc
}

func getName(metric metricType) string {
	// The only way I could find to get name from a metric was this, which is not pretty
	return strings.Split(metric.Desc().String(), `"`)[1]
}
