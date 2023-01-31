package com.amazon.aoc.models.cwmodel;

import lombok.*;

import java.util.List;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dimension {
  private String name;
  private List<String> value;
}
