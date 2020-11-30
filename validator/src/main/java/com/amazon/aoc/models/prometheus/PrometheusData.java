package com.amazon.aoc.models.prometheus;

import lombok.Data;

import java.util.List;

@Data
public class PrometheusData {
  private String resultType;
  private List<PrometheusMetric> result;
}
