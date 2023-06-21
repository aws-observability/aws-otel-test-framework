package com.amazon.aoc.models.prometheus;

import java.util.List;
import lombok.Data;

@Data
public class PrometheusData {
    private String resultType;
    private List<PrometheusMetric> result;
}
