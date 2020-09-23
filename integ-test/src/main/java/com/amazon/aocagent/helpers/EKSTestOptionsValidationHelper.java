/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.aocagent.helpers;

import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.fileconfigs.EksKubeConfigTemplate;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.services.EKSService;
import com.amazonaws.services.eks.model.Cluster;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

@Log4j2
public class EKSTestOptionsValidationHelper {
  /**
   * validate EKS test options.
   *
   * @param context test context
   */
  public static void checkEKSTestOptions(Context context) throws Exception {
    if (context.getEksClusterName() == null && context.getKubeconfigPath() == null) {
      throw new BaseException(ExceptionCode.EKS_CLUSTER_NAME_UNAVAIL);
    }

    if (context.getKubectlPath() == null) {
      // set default kubectl path
      downloadKubectl(context);
    }

    if (context.getIamAuthenticatorPath() == null) {
      // set default aws-iam-authenticator path
      downloadIamAuthenticator(context);
    }

    if (context.getKubeconfigPath() == null) {
      // generate default kubeconfig. It requires a valid "iamAuthenticatorPath" in context
      generateKubeconfig(context);
    }
  }

  private static void generateKubeconfig(Context context) throws Exception {
    // composite kubeconfig
    Cluster cluster = new EKSService(context.getStack().getTestingRegion()).getCluster(context);
    context.setEksCertificate(cluster.getCertificateAuthority().getData());
    context.setEksEndpoint(cluster.getEndpoint());
    String kubeConfigContent =
        new MustacheHelper().render(EksKubeConfigTemplate.KUBE_CONFIG_TEMPLATE, context);

    log.info("kubeConfigContent: \n" + kubeConfigContent);

    File kubeconfig = new File(context.getEksTestArtifactsDir().getPath().toFile(), "kubeconfig");
    FileUtils.writeStringToFile(kubeconfig, kubeConfigContent);
    context.setKubeconfigPath(kubeconfig.getPath());
  }

  private static void downloadKubectl(Context context) throws Exception {
    String urlTemplate =
        "https://amazon-eks.s3.us-west-2.amazonaws.com/1.17.9/2020-08-04/bin/%s/amd64/%s";

    String binaryName = null;
    String os = null;

    if (OSHelper.isLinux()) {
      binaryName = "kubectl";
      os = "linux";
    }

    if (OSHelper.isMac()) {
      binaryName = "kubectl";
      os = "darwin";
    }

    if (os != null) {
      context.setKubectlPath(
          downloadBinary(String.format(urlTemplate, os, binaryName), binaryName, context));
    } else {
      throw new BaseException(ExceptionCode.EKS_KUBECTL_PATH_UNAVAIL);
    }
  }

  private static void downloadIamAuthenticator(Context context) throws Exception {
    String urlTemplate =
        "https://amazon-eks.s3.us-west-2.amazonaws.com/1.17.9/2020-08-04/bin/%s/amd64/%s";

    String binaryName = null;
    String os = null;

    if (OSHelper.isLinux()) {
      binaryName = "aws-iam-authenticator";
      os = "linux";
    }

    if (OSHelper.isMac()) {
      binaryName = "aws-iam-authenticator";
      os = "darwin";
    }

    if (os != null) {
      context.setIamAuthenticatorPath(
          downloadBinary(String.format(urlTemplate, os, binaryName), binaryName, context));
    } else {
      throw new BaseException(ExceptionCode.EKS_IAM_AUTHENTICATOR_PATH_UNAVAIL);
    }
  }

  private static String downloadBinary(String binaryUrl, String binaryName, Context context)
      throws IOException {
    File binaryFile = new File(context.getEksTestArtifactsDir().getPath().toFile(), binaryName);

    FileUtils.copyURLToFile(new URL(binaryUrl), binaryFile);
    binaryFile.setExecutable(true);
    return binaryFile.getPath();
  }
}
