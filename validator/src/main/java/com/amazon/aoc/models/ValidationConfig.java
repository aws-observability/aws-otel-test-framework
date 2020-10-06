package com.amazon.aoc.models;

import com.amazon.aoc.fileconfigs.ExpectedMetric;
import com.amazon.aoc.fileconfigs.ExpectedTrace;
import lombok.Data;

@Data
public class ValidationConfig {
  String validationType;
  String callingType;

  String httpPath;
  String httpMethod;

  ExpectedMetric expectedMetricTemplate;
  ExpectedTrace expectedTraceTemplate;
}
