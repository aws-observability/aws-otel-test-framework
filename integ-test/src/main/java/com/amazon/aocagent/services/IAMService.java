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

package com.amazon.aocagent.services;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleResult;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class IAMService {
  private static final String DNS_SUFFIX = ".amazonaws.com";
  private static final String assumeEC2RolePolicyDoc =
      "{\"Version\":\"2012-10-17\","
          + "\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":[\"ec2"
          + DNS_SUFFIX
          + "\", \"ecs-tasks" + DNS_SUFFIX + "\"]},\"Action\":[\"sts:AssumeRole\"]}]}";
  private AmazonIdentityManagement amazonIdentityManagement;
  private Region region;

  /**
   * Construct IAMService with region.
   *
   * @param region the region to build IAM
   */
  public IAMService(String region) {
    amazonIdentityManagement =
        AmazonIdentityManagementClientBuilder.standard().withRegion(region).build();
    this.region = Region.getRegion(Regions.fromName(region));
  }

  /**
   * createIAMRoleIfNotExisted creates/returns the iam role if it's not existed.
   *
   * @param iamRoleName the iam role name
   * @return the iam role arn.
   */
  public String createIAMRoleIfNotExisted(String iamRoleName) {
    try {
      return getRoleArn(iamRoleName);
    } catch (NoSuchEntityException ex) {
      return createIAMRole(iamRoleName);
    }
  }

  /**
   * get IAM role arn per role name.
   * @param iamRoleName role name
   * @return role arn
   */
  public String getRoleArn(String iamRoleName) {
    GetRoleResult getRoleResult =
        amazonIdentityManagement.getRole(new GetRoleRequest().withRoleName(iamRoleName));

    return getRoleResult.getRole().getArn();
  }

  private String createIAMRole(String iamRoleName) {
    CreateRoleRequest createRoleRequest = new CreateRoleRequest();
    createRoleRequest.setDescription("Used for AOC integration tests.");
    createRoleRequest.setPath("/");
    createRoleRequest.setRoleName(iamRoleName);
    createRoleRequest.setMaxSessionDuration(3600); // 1 hour
    createRoleRequest.setAssumeRolePolicyDocument(assumeEC2RolePolicyDoc);

    CreateRoleResult createRoleResult = amazonIdentityManagement.createRole(createRoleRequest);
    final String roleArn = createRoleResult.getRole().getArn();

    AttachRolePolicyRequest attachRolePolicyRequest = new AttachRolePolicyRequest();
    attachRolePolicyRequest.setRoleName(iamRoleName);

    attachRolePolicyRequest.setPolicyArn(
        String.format("arn:%s:iam::aws:policy/AdministratorAccess", region.getPartition()));
    amazonIdentityManagement.attachRolePolicy(attachRolePolicyRequest);

    CreateInstanceProfileRequest createInstanceProfileRequest = new CreateInstanceProfileRequest();
    createInstanceProfileRequest.setInstanceProfileName(iamRoleName);
    createInstanceProfileRequest.setPath("/");
    amazonIdentityManagement.createInstanceProfile(createInstanceProfileRequest);

    AddRoleToInstanceProfileRequest addRoleToInstanceProfileRequest =
        new AddRoleToInstanceProfileRequest();
    addRoleToInstanceProfileRequest.setRoleName(iamRoleName);
    addRoleToInstanceProfileRequest.setInstanceProfileName(iamRoleName);
    amazonIdentityManagement.addRoleToInstanceProfile(addRoleToInstanceProfileRequest);

    return roleArn;
  }
}
