package com.amazon.aoc.models.prometheus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Data
public class PrometheusMetric {
  private static final String HISTOGRAM_BOUND_FIELD = "le";
  private static final String KEY_FIELD = "key";
  private static final String INFINITE_BOUND = "+Inf";
  private static final String NAME_FIELD = "__name__";
  private static final String QUANTILE_FIELD = "quantile";

  @JsonProperty("metric")
  private Map<String, String> labels;

  @JsonProperty("value")
  private List<String> rawValue;

  public String getMetricName() {
    return labels.get(NAME_FIELD);
  }

  public String getMetricTimestamp() {
    return rawValue.get(0);
  }

  public String getMetricValue() {
    BigDecimal bd = new BigDecimal(rawValue.get(1));
    return bd.setScale(3, RoundingMode.HALF_UP).toPlainString();
  }

  /**
   * Check if a metric should be skipped (when validating). ADOT adds certain metrics
   * when converting Prometheus metrics to the OTLP dataformat (such as the +Inf bound
   * histogram).
   *
   * @return true if metric can be skipped
   */
  public boolean isSkippedMetric() {
    if (labels.containsKey(HISTOGRAM_BOUND_FIELD)) {
      return labels.get(HISTOGRAM_BOUND_FIELD).equals(INFINITE_BOUND);
    }
    return false;
  }

  /**
   * Comparator for comparing two Prometheus metrics by the metric name and labels.
   */
  public static class MetricLabelsComparator implements Comparator<PrometheusMetric> {
    public MetricLabelsComparator() {
    }

    @Override
    public int compare(PrometheusMetric o1, PrometheusMetric o2) {
      // check metric name
      if (!o1.getMetricName().equals(o2.getMetricName())) {
        return o1.getMetricName().compareTo(o2.getMetricName());
      }

      // sort and check labels
      List<String> labelsList1 = labelsMapToList(o1.getLabels());
      List<String> labelsList2 = labelsMapToList(o2.getLabels());

      return labelsList1.toString().compareTo(labelsList2.toString());
    }
  }

  /**
   * Strict comparator for comparing two Prometheus metrics by the metric name, labels and values.
   */
  public static class StrictMetricComparator implements Comparator<PrometheusMetric> {
    public StrictMetricComparator() {
    }

    @Override
    public int compare(PrometheusMetric o1, PrometheusMetric o2) {
      {
        // check metric name
        if (!o1.getMetricName().equals(o2.getMetricName())) {
          return o1.getMetricName().compareTo(o2.getMetricName());
        }

        // check metric value
        if (o1.getMetricValue().equals(o2.getMetricValue())) {
          return o1.getMetricValue().compareTo(o2.getMetricValue());
        }

        // sort and check labels
        List<String> labelsList1 = labelsMapToList(o1.getLabels());
        List<String> labelsList2 = labelsMapToList(o2.getLabels());

        return labelsList1.toString().compareTo(labelsList2.toString());
      }
    }
  }

  private static List<String> labelsMapToList(Map<String, String> labelsMap) {
    List<String> labelsList = new ArrayList<>();
    for (Map.Entry<String, String> entry : labelsMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      // skip validation of labels that ADOT may add (we only keep labels created by
      // prometheus or by the sample app). All sample app metrics will be prefixed
      // by 'key'.
      if (!key.equals(HISTOGRAM_BOUND_FIELD)
              && !key.equals(QUANTILE_FIELD)
              && key.indexOf(KEY_FIELD) != 0) {
        continue;
      }

      try {
        BigDecimal bd = new BigDecimal(value);
        value = bd.setScale(3, RoundingMode.HALF_UP).toPlainString();
      } catch (NumberFormatException ignored) {
        continue;
      }

      labelsList.add(key + ":" + value);
    }
    return labelsList;
  }
}
