package com.amazon.aoc.helpers;

import com.amazon.aoc.models.Context;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import java.util.List;
import java.util.Objects;

public class K8sExpectedValuesHelper {

  public void getExpectedMetrics(Context context) throws Exception {
    ApiClient client = Config.fromConfig(context.getKubeCfgFilePath());
    Configuration.setDefaultApiClient(client);

    // get all pods in namespace
    List<V1Pod> pods =
        Kubectl.get(V1Pod.class).namespace(context.getKubernetesContext().getNamespace()).execute();

    // Go through pods until we find the first one starting with "sample-app-".
    // Set expected values in kubernetesContext once found then exit.
    for (V1Pod pod : pods) {
      if (Objects.requireNonNull(Objects.requireNonNull(pod.getMetadata()).getName())
          .startsWith("sample-app-")) {
        context.getKubernetesContext().setNamespace(pod.getMetadata().getNamespace());
        context.getKubernetesContext().setPodName(pod.getMetadata().getName());
        context.getKubernetesContext().setPodUid(pod.getMetadata().getUid());
        context
            .getKubernetesContext()
            .setNodeName(Objects.requireNonNull(pod.getSpec()).getNodeName());
        System.out.println("start time: " + pod.getMetadata().getCreationTimestamp());
        // need to convert this to the same format that the processor uses to export timestamps
        context
            .getKubernetesContext()
            .setCreationTimeStamp(
                Objects.requireNonNull(pod.getMetadata().getCreationTimestamp()).toString());
        break;
      }
    }
  }
}
