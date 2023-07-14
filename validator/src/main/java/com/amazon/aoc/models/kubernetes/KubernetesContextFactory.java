package com.amazon.aoc.models.kubernetes;

import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import java.util.List;
import java.util.Objects;

public class KubernetesContextFactory {
  private final String kubeConfigFilePath;
  private final String deploymentName;

  private final String namespace;

  public KubernetesContextFactory(
      String kubeConfigFilePath, String deploymentName, String namespace) {
    this.kubeConfigFilePath = kubeConfigFilePath;
    this.deploymentName = deploymentName;
    this.namespace = namespace;
  }

  public KubernetesContext create() throws Exception {
    KubernetesContext kubernetesContext =
        new KubernetesContext(this.deploymentName, this.namespace);
    ApiClient client = Config.fromConfig(this.kubeConfigFilePath);
    Configuration.setDefaultApiClient(client);

    // get all pods in namespace
    List<V1Pod> pods = Kubectl.get(V1Pod.class).namespace(this.namespace).execute();

    // Go through pods until we find the first one starting with "sample-app-".
    // Set expected values in kubernetesContext once found then exit.
    for (V1Pod pod : pods) {
      if (Objects.requireNonNull(Objects.requireNonNull(pod.getMetadata()).getName())
          .startsWith(this.deploymentName)) {
        kubernetesContext.setNamespace(pod.getMetadata().getNamespace());
        kubernetesContext.setPodName(pod.getMetadata().getName());
        kubernetesContext.setPodUid(pod.getMetadata().getUid());
        kubernetesContext.setNodeName(Objects.requireNonNull(pod.getSpec()).getNodeName());

        /*
        Currently waiting for RF3339 format to be used before asserting against creation timestamps.
        https://github.com/open-telemetry/opentelemetry-collector-contrib/pull/24016
          String creationTimestamp =
            pod.getMetadata()
                .getCreationTimestamp()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xx 'UTC'"));

        context.getKubernetesContext().setCreationTimeStamp(creationTimestamp);
         */

        break;
      }
    }
    return kubernetesContext;
  }
}
