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

package com.amazon.aocagent.tasks;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.helpers.CommandExecutionHelper;
import com.amazon.aocagent.helpers.EKSTestOptionsValidationHelper;
import com.amazon.aocagent.helpers.TempDirHelper;
import com.amazon.aocagent.models.Context;
import lombok.extern.log4j.Log4j2;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class EKSClean implements ITask {
  private Context context;

  @Override
  public void init(Context context) throws Exception {
    context.setEksTestArtifactsDir(new TempDirHelper(GenericConstants.EKS_INTEG_TEST.getVal()));
    EKSTestOptionsValidationHelper.checkEKSTestOptions(context);
    this.context = context;
  }

  @Override
  public void execute() throws Exception {
    cleanNamespaces();
    TempDirHelper.cleanTempDirs();
  }

  @Override
  public void clean() throws Exception {
    context.getEksTestArtifactsDir().deletePath();
  }

  private void cleanNamespaces() throws BaseException {
    String command =
        String.format(
            "%s get ns --kubeconfig %s", context.getKubectlPath(), context.getKubeconfigPath());
    String result = CommandExecutionHelper.runChildProcessWithAWSCred(command);

    List<String> namespaces = new ArrayList<>();
    String[] lines = result.split("\n");
    for (String line : lines) {
      if (line.startsWith("eks-integ-test-")) {
        // line example: "eks-integ-test-1599881334414   Active   10s"
        String namespace = line.split(" ")[0];
        // extract creation time (in milliseconds) of the namespace, i.e.
        // eks-integ-test-1599881334414
        String[] elements = namespace.split("-", 4);
        if (elements.length == 4) {
          String timestamp = elements[3];
          // add to target namespaces if it was created 2 hours ago
          if (new Date(Long.parseLong(timestamp))
              .before(
                  new DateTime()
                      .minusMinutes(
                          Integer.parseInt(GenericConstants.RESOURCE_CLEAN_THRESHOLD.getVal()))
                      .toDate())) {
            namespaces.add(namespace);
          }
        }
      }
    }

    log.info("Deleting old namespaces {}", namespaces);
    for (String namespace : namespaces) {
      command =
          String.format(
              "%s delete ns %s --kubeconfig %s",
              context.getKubectlPath(), namespace, context.getKubeconfigPath());
      CommandExecutionHelper.runChildProcessWithAWSCred(command);
      log.info("namespace {} has been deleted !", namespace);
    }
  }
}
