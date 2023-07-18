package com.amazon.aoc.services;

import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/*
 * By default, creates a kubernetes client using the provided kubernetes config file path
 */
public class KubernetesService {

  public KubernetesService(String kubecfgFilePath) throws IOException {
    Configuration.setDefaultApiClient(Config.fromConfig(kubecfgFilePath));
  }

  public V1Pod getSampleAppPod(String deploymentName, String namespace) throws Exception {
    List<V1Pod> pods = Kubectl.get(V1Pod.class).namespace(namespace).execute();
    for (V1Pod pod : pods) {
      if (Objects.requireNonNull(Objects.requireNonNull(pod.getMetadata()).getName())
          .startsWith(deploymentName)) {
        return pod;
      }
    }
    return null;
  }
}
