package com.amazon.aoc.services;

import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.ApiClient;
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
  private final String kubeConfigFilePath;

  public KubernetesService(String kubecfgfp) throws IOException {
    this.kubeConfigFilePath = kubecfgfp;
    Configuration.setDefaultApiClient(buildK8sAPIClient());
  }

  private ApiClient buildK8sAPIClient() throws IOException {
    return Config.fromConfig(this.kubeConfigFilePath);
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
