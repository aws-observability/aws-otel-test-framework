package com.amazon.aoc.models;

import lombok.Data;

@Data
public class ECSContext {
  // ecs related context
  private String ecsClusterName;
  private String ecsTaskArn;
  private String ecsTaskDefFamily;
  private String ecsTaskDefVersion;
}
