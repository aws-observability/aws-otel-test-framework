package com.amazon.aoc.models.prometheus;

import lombok.Data;

@Data
public class PrometheusQueryResult {
  private String status;
  private PrometheusData data;
}
