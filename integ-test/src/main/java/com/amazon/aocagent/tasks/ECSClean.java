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
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.services.EC2Service;
import com.amazon.aocagent.services.ECSService;
import com.amazonaws.services.ec2.model.Instance;
import lombok.extern.log4j.Log4j2;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Log4j2
public class ECSClean implements ITask {
  private EC2Service ec2Service;
  private ECSService ecsService;

  @Override
  public void init(Context context) throws Exception {
    this.ec2Service = new EC2Service(context.getStack().getTestingRegion());
    this.ecsService = new ECSService(context.getStack().getTestingRegion());
  }

  @Override
  public void execute() throws Exception {
    cleanTasks();
    cleanContainerInstances();
    cleanCluster();
    cleanTaskDefinitions();
  }

  private void cleanTaskDefinitions() {
    ecsService.cleanTaskDefinitions(GenericConstants.AOC_PREFIX.getVal());
  }

  private void cleanCluster() {
    ecsService.cleanCluster();
  }

  private void cleanContainerInstances() throws Exception {
    List<Instance> instanceList =
        ec2Service.listInstancesByTag(
            GenericConstants.EC2_INSTANCE_TAG_KEY.getVal(),
            GenericConstants.EC2_INSTANCE_ECS_TAG_VAL.getVal());
    // filter instance older than 2 hours ago
    List<String> instanceIdListToBeTerminated = new ArrayList<>();

    instanceList.forEach(
        instance -> {
          if (instance
                  .getLaunchTime()
                  .before(
                      new DateTime()
                          .minusMinutes(
                              Integer.parseInt(GenericConstants.RESOURCE_CLEAN_THRESHOLD.getVal()))
                          .toDate())
              && instance.getTags().size() == 1) {
            instanceIdListToBeTerminated.add(instance.getInstanceId());
          }
        });

    log.info("terminating unused ec2 instances: {}", instanceIdListToBeTerminated);
    // terminate instance
    ec2Service.terminateInstance(instanceIdListToBeTerminated);
  }

  private void cleanTasks() throws Exception {
    ecsService.cleanTasks();
  }
}
