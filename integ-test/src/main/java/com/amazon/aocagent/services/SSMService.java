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

import com.amazon.aocagent.enums.GenericConstants;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.Command;
import com.amazonaws.services.simplesystemsmanagement.model.CommandPlugin;
import com.amazonaws.services.simplesystemsmanagement.model.CommandStatus;
import com.amazonaws.services.simplesystemsmanagement.model.DescribeInstanceInformationRequest;
import com.amazonaws.services.simplesystemsmanagement.model.InstanceInformation;
import com.amazonaws.services.simplesystemsmanagement.model.InstanceInformationFilter;
import com.amazonaws.services.simplesystemsmanagement.model.InvalidInstanceIdException;
import com.amazonaws.services.simplesystemsmanagement.model.ListCommandInvocationsRequest;
import com.amazonaws.services.simplesystemsmanagement.model.ListCommandsRequest;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
public class SSMService {
  private AWSSimpleSystemsManagement ssmClient;

  /**
   * SSMService Constructor.
   *
   * @param region AWS region used to init SSM Service
   */
  public SSMService(String region) throws Exception {
    AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.standard()
            .withRegion(region).build();
    ssmClient = ssm;
  }

  /**
   * updateSsmAgentToLatest.
   *
   * @param instanceId the ID of the instance which needs to update ssm agent
   */
  public void updateSsmAgentToLatest(final String instanceId)
          throws InterruptedException {
    final Command command = ssmClient.sendCommand(new SendCommandRequest()
            .withInstanceIds(Collections.singleton(instanceId))
            .withDocumentName(GenericConstants.UPDATE_SSM_AGENT_DOCUMENT.getVal())
            .withTimeoutSeconds(180)).getCommand();
    trackCommandStatus(command);
  }

  /**
   * runShellScriptCommand.
   *
   * @param instanceId          the ID of the instance which needs to update ssm agent
   * @param commands            list of commands which will be executed by ssm run command
   * @param commandDocumentName SSM document name
   */
  public void runShellScriptCommand(final String instanceId, final List<String> commands,
                                    final String commandDocumentName) throws InterruptedException {
    Map<String, List<String>> params = new HashMap<>();
    params.put("commands", commands);
    final Command command = ssmClient.sendCommand(new SendCommandRequest()
            .withInstanceIds(Collections.singleton(instanceId))
            .withDocumentName(commandDocumentName)
            .withTimeoutSeconds(180)
            .withParameters(params)).getCommand();
    if (!"Success".equals(trackCommandStatus(command))) {
      throw new RuntimeException("On " + instanceId + ", failed to execute the command: "
              + commands);
    }
  }

  private String trackCommandStatus(final Command command) throws InterruptedException {
    log.info("Starting command {} for document {}",
            command.getCommandId(), command.getDocumentName());
    String status = command.getStatus();
    int remainingRetrys = Integer.parseInt(GenericConstants.MAX_RETRIES.getVal());
    while (CommandStatus.fromValue(status) == CommandStatus.Pending
            || CommandStatus.fromValue(status) == CommandStatus.InProgress
            || CommandStatus.fromValue(status) == CommandStatus.Cancelling) {
      if (remainingRetrys <= 0) {
        break;
      }
      remainingRetrys--;
      log.info("Command status : " + status + ". Sleeping for "
              + Integer.parseInt(GenericConstants.SLEEP_IN_SECONDS.getVal()) + " seconds...");
      TimeUnit.SECONDS.sleep(Integer.parseInt(GenericConstants.SLEEP_IN_SECONDS.getVal()));
      status = ssmClient.listCommands(new ListCommandsRequest()
              .withCommandId(command.getCommandId())).getCommands().get(0).getStatus();
    }
    StringBuffer sb = new StringBuffer();
    try {
      List<CommandPlugin> commandPlugins = ssmClient.listCommandInvocations(
              new ListCommandInvocationsRequest()
                      .withCommandId(command.getCommandId()).withDetails(true)
      ).getCommandInvocations().get(0).getCommandPlugins();
      for (CommandPlugin commandPlugin : commandPlugins) {
        sb.append("Name: ");
        sb.append(commandPlugin.getName());
        sb.append("\n");
        sb.append("Output: ");
        sb.append(commandPlugin.getOutput());
        sb.append("\n");
      }
    } catch (Exception e) {
      log.error("Exception happened for ssm listCommandInvocations call", e);
    }
    log.info("Command {} for document {} completed with status {} : \n{}",
            command.getCommandId(), command.getDocumentName(), status, sb.toString());

    return status;
  }

  /**
   * isInstanceReadyForSsm.
   *
   * @param instanceId the ID of the instance which needs to check ssm agent status
   */
  public boolean isInstanceReadyForSsm(final String instanceId) throws Exception {
    if (getInstanceInformation(instanceId) == null) {
      log.info("Instance " + instanceId + " is not ready for SSM yet. Sleeping for "
              + Integer.parseInt(GenericConstants.SLEEP_IN_SECONDS.getVal()) + " seconds...");
      return false;
    }
    log.info("Instance " + instanceId + " is ready for SSM");
    return true;
  }

  private InstanceInformation getInstanceInformation(final String instanceId) {
    try {
      InstanceInformationFilter instanceInformationFilter = new InstanceInformationFilter()
              .withKey("InstanceIds").withValueSet(instanceId);
      DescribeInstanceInformationRequest describeInstanceInformationRequest =
              new DescribeInstanceInformationRequest()
                      .withInstanceInformationFilterList(Collections
                              .singletonList(instanceInformationFilter));

      final List<InstanceInformation> instances =
              ssmClient.describeInstanceInformation(describeInstanceInformationRequest)
                      .getInstanceInformationList();
      return instances.size() > 0 ? instances.get(0) : null;
    } catch (InvalidInstanceIdException ex) {
      log.error("Invalid Instance ID where instance ID is " + instanceId, ex);
      return null;
    }
  }
}