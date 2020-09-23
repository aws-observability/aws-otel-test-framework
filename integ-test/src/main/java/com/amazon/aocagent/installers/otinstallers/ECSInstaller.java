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
import com.amazon.aocagent.helpers.MustacheHelper;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.services.ECSService;
import com.amazon.aocagent.services.IAMService;
import com.amazonaws.services.ecs.model.AssignPublicIp;
import com.amazonaws.services.ecs.model.AwsVpcConfiguration;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.NetworkConfiguration;
import com.amazonaws.services.ecs.model.RunTaskRequest;

public class ECSInstaller implements OTInstaller {
  private Context context;
  private ECSService ecsService;
  private IAMService iamService;
  private MustacheHelper mustacheHelper;

  @Override
  public void init(Context context) throws Exception {
    this.context = context;
    this.ecsService = new ECSService(context.getStack().getTestingRegion());
    this.iamService = new IAMService(context.getStack().getTestingRegion());
    this.mustacheHelper = new MustacheHelper();
  }

  @Override
  public void installAndStart() throws Exception {

    // setup ecs context for filling task definition template
    this.setupEcsContext(context);

    // create and run ECS target task definitions from template
    final String taskDefinitionStr = mustacheHelper.render(context.getEcsTaskDef(), context);

    // register the task definition in ECS
    ecsService.registerTaskDefinition(taskDefinitionStr);

    // create ecs run task request
    RunTaskRequest taskRequest = this.getTaskRequest(this.context);

    // run ECS task
    ecsService.runTaskDefinition(taskRequest);
  }

  private void setupEcsContext(Context context) {
    context.setAocImage(context.getStack().getTestingImageRepoName()
        + ":" + context.getAgentVersion());
    context.setDataEmitterImage(GenericConstants.TRACE_EMITTER_DOCKER_IMAGE_URL.getVal());
    context.setRegion(context.getStack().getTestingRegion());
    String iamRoleArn = this.iamService.getRoleArn(GenericConstants.IAM_ROLE_NAME.getVal());
    context.setEcsTaskRoleArn(iamRoleArn);
    context.setEcsExecutionRoleArn(iamRoleArn);
  }

  private RunTaskRequest getTaskRequest(Context context) {
    String launchType = context.getEcsLaunchType();
    if (launchType.equalsIgnoreCase(GenericConstants.EC2.getVal())) {
      return new RunTaskRequest()
          .withLaunchType(LaunchType.EC2)
          .withTaskDefinition(GenericConstants.AOC_PREFIX.getVal() + launchType)
          .withCluster(context.getEcsClusterName())
          .withCount(1);
    } else {
      return new RunTaskRequest()
          .withLaunchType(LaunchType.FARGATE)
          .withTaskDefinition(GenericConstants.AOC_PREFIX.getVal() + launchType)
          .withCluster(context.getEcsClusterName())
          .withCount(1)
          .withNetworkConfiguration(
              new NetworkConfiguration()
                  .withAwsvpcConfiguration(
                      new AwsVpcConfiguration()
                          .withAssignPublicIp(AssignPublicIp.ENABLED)
                          .withSecurityGroups(context.getDefaultSecurityGrpId())
                          .withSubnets(context.getDefaultSubnets().get(0).getSubnetId())));
    }
  }
}
