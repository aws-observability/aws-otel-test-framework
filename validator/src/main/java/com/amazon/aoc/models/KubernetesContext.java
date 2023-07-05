package com.amazon.aoc.models;

import lombok.Data;

@Data
public class KubernetesContext {
  private String deploymentName;

  private String namespace;

  private String podName;

  private String nodeName;

  private String podUid;

  private String creationTimeStamp;
}
