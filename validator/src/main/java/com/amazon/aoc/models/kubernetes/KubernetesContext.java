package com.amazon.aoc.models.kubernetes;

import lombok.Data;

@Data
public class KubernetesContext {
  private String deploymentName;

  private String namespace;

  private String podName;

  private String nodeName;

  private String podUid;

  // Waiting for RFC3339 format to be used by default
  // private String creationTimeStamp;

  public KubernetesContext(String deploymentName, String namespace) {
    this.deploymentName = deploymentName;
    this.namespace = namespace;
  }

  public KubernetesContext() {}
}
