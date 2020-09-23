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

package com.amazon.aocagent.commands;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.fileconfigs.ECSTaskDefTemplate;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.models.Stack;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@CommandLine.Command(footer = "Common footer")
@Log4j2
public class CommonOption {
  @CommandLine.Option(
      names = {"-l", "--local-packages-dir"},
      description =
          "read packages, version file from this directory, default value is build/packages",
      defaultValue = "build/packages")
  private String localPackagesDir;

  @CommandLine.Option(
      names = {"-p", "--package-version"},
      description = "the package version, fetched from local-packages-dir/VERSION by default")
  private String version;

  @CommandLine.Option(
      names = {"-s", "--stack"},
      description = "stack file path, .aoc-stack.yml by default",
      defaultValue = ".aoc-stack.yml")
  private String stackFilePath;

  @CommandLine.Option(
      names = {"-e", "--ecs-context"},
      description = "eg, -e ecsLaunchType=EC2 -e ecsDeployMode=SIDECAR",
      defaultValue = "ecsLaunchType=EC2")
  private Map<String, String> ecsContexts;

  @CommandLine.Option(
      names = {"-k", "--eks-context"},
      description =
          "eg, -k eksClusterName=my-cluster-name "
              + "-k kubectlPath=/my/kubectl/path "
              + "-k kubeconfigPath=/my/kubeconfig/path "
              + "-k awsAuthenticatorPath=/my/authenticator/path "
              + "-k eksTestManifestName=testManifest")
  private Map<String, String> eksContexts;

  /**
   * buildContext build the context object based on the command args.
   *
   * @return Context
   * @throws IOException when the VERSION file is not found
   */
  public Context buildContext() throws IOException, BaseException {
    // build stack
    Stack stack = this.buildStack();

    Context context = new Context();

    context.setStack(stack);
    context.setStackFilePath(stackFilePath);

    // local package dir
    context.setLocalPackagesDir(this.localPackagesDir);

    // get aoc version from the current working directory: "build/packages/VERSION"
    if (StringUtils.isNullOrEmpty(this.version)) {
      this.version =
          new String(
                  Files.readAllBytes(Paths.get(this.localPackagesDir + "/VERSION")),
                  StandardCharsets.UTF_8)
              .trim();
    }
    context.setAgentVersion(this.version);

    setExtraContext(ecsContexts, context);
    setExtraContext(eksContexts, context);
    return context;
  }

  private Stack buildStack() throws IOException, BaseException {
    // read stack from .aoc-stack
    if (!Files.exists(Paths.get(this.stackFilePath))) {
      throw new BaseException(ExceptionCode.STACK_FILE_NOT_FOUND);
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(
        new String(Files.readAllBytes(Paths.get(this.stackFilePath))), Stack.class);
  }

  private void setExtraContext(Map<String, String> extra, Context context) {
    if (extra != null) {
      extra
          .entrySet()
          .forEach(
              e -> {
                if (e.getKey().equals(GenericConstants.ECS_LAUNCH_TYPE.getVal())) {
                  context.setEcsLaunchType(e.getValue());
                } else if (e.getKey().equals(GenericConstants.ECS_DEPLOY_MODE.getVal())) {
                  context.setEcsDeploymentMode(e.getValue());
                } else if (e.getKey().equals(GenericConstants.ECS_TASK_DEF.getVal())) {
                  context.setEcsTaskDef(ECSTaskDefTemplate.valueOf(e.getValue()));
                } else if (e.getKey().equals(GenericConstants.AUTHENTICATOR_PATH.getVal())) {
                  context.setIamAuthenticatorPath(e.getValue());
                } else if (e.getKey().equals(GenericConstants.EKS_CLUSTER_NAME.getVal())) {
                  context.setEksClusterName(e.getValue());
                } else if (e.getKey().equals(GenericConstants.KUBECTL_PATH.getVal())) {
                  context.setKubectlPath(e.getValue());
                } else if (e.getKey().equals(GenericConstants.KUBECONFIG_PATH.getVal())) {
                  context.setKubeconfigPath(e.getValue());
                } else if (e.getKey().equals(GenericConstants.TEST_MANIFEST_NAME.getVal())) {
                  context.setEksTestManifestName(e.getValue());
                }
              });
    }
  }
}
