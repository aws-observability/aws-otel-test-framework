package com.amazon.aoc.helpers;

import com.amazon.aoc.models.Context;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public class K8sExpectedValuesHelper {

  public static void populateExpectedMetrics(Context context) throws Exception {
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
  }
}
