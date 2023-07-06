package com.amazon.aoc.validators;

import java.util.Arrays;
import java.util.List;

public class ContainerInsightStructuredLogValidatorContainerd
    extends ContainerInsightStructuredLogValidatorBase {

  // expected template Array for containerd runtime
  @Override
  public List<String> getLogTypeToValidate() {
    return Arrays.asList(
        "Cluster",
        "ClusterNamespace",
        "ClusterService",
        "Container",
        "Node",
        "NodeDiskIO",
        "NodeFS",
        "NodeNet",
        "Pod",
        "PodNet");
  }
}
