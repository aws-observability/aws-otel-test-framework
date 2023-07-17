package com.amazon.aoc.models.kubernetes;

import com.amazon.aoc.helpers.KubernetesMetadataHelper;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.Objects;

public class KubernetesContextFactory {
  private final String kubeConfigFilePath;
  private final String deploymentName;

  private final String namespace;

  private final KubernetesMetadataHelper kubernetesMetadataHelper;

  public KubernetesContextFactory(
      String kubeConfigFilePath, String deploymentName, String namespace) throws Exception {
    this.kubeConfigFilePath = kubeConfigFilePath;
    this.deploymentName = deploymentName;
    this.namespace = namespace;
    this.kubernetesMetadataHelper = new KubernetesMetadataHelper(this.kubeConfigFilePath);
  }

  public KubernetesContextFactory(
      String kubeConfigFilePath,
      String deploymentName,
      String namespace,
      KubernetesMetadataHelper kubernetesMetadataHelper) {
    this.kubeConfigFilePath = kubeConfigFilePath;
    this.deploymentName = deploymentName;
    this.namespace = namespace;
    this.kubernetesMetadataHelper = kubernetesMetadataHelper;
  }

  public KubernetesContext create() throws Exception {
    KubernetesContext kubernetesContext =
        new KubernetesContext(this.deploymentName, this.namespace);

    V1Pod pod = kubernetesMetadataHelper.getSampleAppPod(this.deploymentName, this.namespace);
    if (pod != null) {
      kubernetesContext.setNamespace(Objects.requireNonNull(pod.getMetadata()).getNamespace());
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
    }

    return kubernetesContext;
  }
}
