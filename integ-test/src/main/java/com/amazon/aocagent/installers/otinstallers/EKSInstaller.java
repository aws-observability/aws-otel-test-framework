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
package com.amazon.aocagent.installers.otinstallers;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.fileconfigs.EksSidecarManifestTemplate;
import com.amazon.aocagent.helpers.CommandExecutionHelper;
import com.amazon.aocagent.helpers.MustacheHelper;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.services.EKSService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.File;

@Log4j2
public class EKSInstaller implements OTInstaller {
  private Context context;
  private EKSService eksService;
  private MustacheHelper mustacheHelper;

  @Override
  public void init(Context context) throws Exception {
    this.context = context;
    this.eksService = new EKSService(context.getStack().getTestingRegion());
    this.mustacheHelper = new MustacheHelper();
  }

  @Override
  public void installAndStart() throws Exception {
    setupEKSContext(context);
    generateEKSTestManifestFile(context);
    deployEKSTestManifestFile(context);

    log.info("EKS integ test {} has been deployed", context.getEksTestManifestName());
  }

  private void setupEKSContext(Context context) {
    context.setAocImage(
        context.getStack().getTestingImageRepoName() + ":" + context.getAgentVersion());
    context.setDataEmitterImage(GenericConstants.TRACE_EMITTER_DOCKER_IMAGE_URL.getVal());
    // Uses current timestamp as instance id which is used as a uniq test id
    context.setInstanceId(String.valueOf(System.currentTimeMillis()));
    if (context.getEksTestManifestName() == null) {
      context.setEksTestManifestName(GenericConstants.EKS_DEFAULT_TEST_MANIFEST.getVal());
    }
  }

  private void generateEKSTestManifestFile(Context context) throws Exception {
    String manifestYamlContent =
        mustacheHelper.render(
            new EksSidecarManifestTemplate(
                String.format("/templates/eks/%s.mustache", context.getEksTestManifestName())),
            context);

    log.info("EKS sidecar integ test deployment yaml content:\n" + manifestYamlContent);

    FileUtils.writeStringToFile(
        new File(
            context.getEksTestArtifactsDir().getPath().toFile(),
            String.format("%s.yml", context.getEksTestManifestName())),
        manifestYamlContent);
  }

  private void deployEKSTestManifestFile(Context context) throws Exception {
    String command =
        String.format(
            "%s apply -f %s --kubeconfig %s",
            context.getKubectlPath(),
            new File(
                    context.getEksTestArtifactsDir().getPath().toFile(),
                    String.format("%s.yml", context.getEksTestManifestName()))
                .getPath(),
            context.getKubeconfigPath());
    CommandExecutionHelper.runChildProcessWithAWSCred(command);
  }
}
