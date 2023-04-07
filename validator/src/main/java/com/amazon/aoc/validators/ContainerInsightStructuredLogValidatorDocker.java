package com.amazon.aoc.validators;

import java.util.Arrays;
import java.util.List;


public class ContainerInsightStructuredLogValidatorDocker
        extends ContainerInsightStructuredLogValidatorBase {

  //expected template Array for docker runtime
  @Override
  public List<String> getLogTypeToValidate() {
    return Arrays.asList(
      "Cluster",
      "ClusterNamespace",
      "ClusterService",
      "Container",
      "ContainerFS",
      "Node",
      "NodeDiskIO",
      "NodeFS",
      "NodeNet",
      "Pod",
      "PodNet"
    );
  }

}
