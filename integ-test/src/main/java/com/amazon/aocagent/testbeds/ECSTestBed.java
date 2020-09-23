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
package com.amazon.aocagent.testbeds;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.models.EC2InstanceParams;
import com.amazon.aocagent.services.AwsNetworkService;
import com.amazon.aocagent.services.EC2Service;
import com.amazon.aocagent.services.ECSService;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import lombok.extern.log4j.Log4j2;

import java.util.Base64;

@Log4j2
public class ECSTestBed implements TestBed {

  private ECSService ecsService;
  private EC2Service ec2Service;
  private AwsNetworkService networkService;
  private Context context;

  /** assign the container instance to the specified ECS cluster. */
  private static String CONTAINER_INSTANCE_USER_DATA =
      "#!/bin/bash\n" + "echo ECS_CLUSTER=%s >> /etc/ecs/ecs.config";

  @Override
  public void init(Context context) throws Exception {
    this.context = context;
    this.ecsService = new ECSService(context.getStack().getTestingRegion());
    this.ec2Service = new EC2Service(context.getStack().getTestingRegion());
    this.networkService = new AwsNetworkService(context.getStack().getTestingRegion());
  }

  /**
   * run AOC and data emitter on ECS fargate and EC2 instances.
   * @return context params after setup ECS test bed
   * @throws Exception failed to launch testbed
   */
  @Override
  public Context launchTestBed() throws Exception {
    // ECS uses current timestamp as instance id
    context.setInstanceId(String.valueOf(System.currentTimeMillis()));
    // create ECS cluster
    final String clusterName = this.generateEcsClusterName(context);
    this.context.setEcsClusterName(clusterName);
    if (!ecsService.describeCluster(clusterName).isPresent()) {
      ecsService.createCluster(clusterName);
    }
    // get the default security group, vpc and subnets
    // from the provided aws account
    this.buildNetworkContext(context);

    // launch new EC2 container instance for EC2 mode
    if (context.getEcsLaunchType().equalsIgnoreCase(GenericConstants.EC2.getVal())
        && !ecsService.isContainerInstanceAvail(clusterName)) {
      log.info("launching up a container instance");
      EC2InstanceParams ec2InstanceParams = this.buildEc2ConfigForEcs(context);
      Instance containerInstance = ec2Service.launchInstance(ec2InstanceParams);
      log.info(
          "created new ECS container instance: {} - {} ",
          containerInstance.getInstanceId(),
          containerInstance.getState().getName());
      ecsService.waitForContainerInstanceRegistered(clusterName);
    }

    return this.context;
  }

  private void buildNetworkContext(Context context) throws Exception {
    DescribeSecurityGroupsResult securityGroupsResult =
        networkService.describeDefaultSecurityGroup();
    SecurityGroup defaultGroup = securityGroupsResult.getSecurityGroups().get(0);
    context.setDefaultSecurityGrpId(defaultGroup.getGroupId());
    context.setDefaultVpcId(defaultGroup.getVpcId());

    DescribeSubnetsResult subnetsResult =
        networkService.describeVpcSubnets(context.getDefaultVpcId());
    context.setDefaultSubnets(subnetsResult.getSubnets());
  }

  private String generateEcsClusterName(Context context) {
    return GenericConstants.ECS_SIDECAR_CLUSTER.getVal() + "-" + context.getInstanceId();
  }

  /**
   * build launching config for EC2 container instance.
   * @param context test context
   * @return {@link EC2InstanceParams} ecs launch params
   */
  private EC2InstanceParams buildEc2ConfigForEcs(Context context) {
    // tag instance for management
    TagSpecification tagSpecification =
        new TagSpecification()
            .withResourceType(ResourceType.Instance)
            .withTags(
                new Tag(
                    GenericConstants.EC2_INSTANCE_TAG_KEY.getVal(),
                    GenericConstants.EC2_INSTANCE_ECS_TAG_VAL.getVal()));
    String userData =
        Base64.getEncoder()
            .encodeToString(
                String.format(
                    CONTAINER_INSTANCE_USER_DATA, context.getEcsClusterName())
                    .getBytes());
    return EC2InstanceParams.builder()
        .amiId(context.getTestingAMI().getAMIId())
        .instanceType(context.getTestingAMI().getInstanceType())
        .iamRoleName(GenericConstants.IAM_ROLE_NAME.getVal())
        .securityGrpName(GenericConstants.DEFAULT.getVal())
        .tagSpecification(tagSpecification)
        .sshKeyName(GenericConstants.SSH_KEY_NAME.getVal())
        .userData(userData)
        .build();
  }
}
