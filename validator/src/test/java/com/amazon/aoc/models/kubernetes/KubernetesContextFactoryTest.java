package com.amazon.aoc.models.kubernetes;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.amazon.aoc.services.KubernetesService;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import org.junit.Test;

public class KubernetesContextFactoryTest {

  @Test
  public void testCreateContextHappyPath() throws Exception {
    String mockDeploymentName = "mock-deployment";
    String mockNamespace = "mock-namespace";
    String mockUid = "1234";
    String mockNodeName = "im-a-node-name-1235";
    V1Pod mockPod =
        new V1Pod()
            .metadata(
                new V1ObjectMeta()
                    .name(mockDeploymentName + "-1290344")
                    .namespace(mockNamespace)
                    .uid(mockUid))
            .spec(new V1PodSpec().nodeName(mockNodeName));
    KubernetesService mockKubernetesService = mock(KubernetesService.class);
    when(mockKubernetesService.getSampleAppPod(mockDeploymentName, mockNamespace))
        .thenReturn(mockPod);
    KubernetesContextFactory mockFactory =
        new KubernetesContextFactory(
            "./kubecfg", mockDeploymentName, mockNamespace, mockKubernetesService);

    KubernetesContext actualKubernetesContext = mockFactory.create();

    assertEquals(mockDeploymentName, actualKubernetesContext.getDeploymentName());
    assertEquals(mockNamespace, actualKubernetesContext.getNamespace());
    assertEquals(mockUid, actualKubernetesContext.getPodUid());
    assertEquals(mockNodeName, actualKubernetesContext.getNodeName());
  }
}
