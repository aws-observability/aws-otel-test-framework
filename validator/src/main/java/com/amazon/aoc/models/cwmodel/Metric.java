package com.amazon.aoc.models.cwmodel;


import lombok.*;

import java.util.List;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Metric {
  private String metricName;
  private String namespace;
  private List<Dimension> dimensions;
}
