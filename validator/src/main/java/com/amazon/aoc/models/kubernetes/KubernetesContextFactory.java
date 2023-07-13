package com.amazon.aoc.models.kubernetes;

import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public class KubernetesContextFactory {
  private String kubeConfigFilePath;
  private String deploymentName;

  private String namespace;

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
          .startsWith("sample-app-")) {
        kubernetesContext.setNamespace(pod.getMetadata().getNamespace());
        kubernetesContext.setPodName(pod.getMetadata().getName());
        kubernetesContext.setPodUid(pod.getMetadata().getUid());
        kubernetesContext.setNodeName(Objects.requireNonNull(pod.getSpec()).getNodeName());

        OffsetDateTime test = pod.getMetadata().getCreationTimestamp();
        /*
        This datetimeformatter hard codes the UTC zone to match the datetime format exported by the k8sattr processor
        and the k8s api go package https://pkg.go.dev/k8s.io/apimachinery/pkg/apis/meta/v1@v0.27.1#Time
        Attempting to use a pattern such as ofPattern("yyyy-MM-dd HH:mm:ss xxx v") throws an error
        because the timezone cannot be inferred. For example "Unable to extract ZoneId from temporal 2023-07-06T00:35:31Z"
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
