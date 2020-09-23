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

import com.amazon.aocagent.installers.emiterinstallers.OTEmitterInstaller;
import com.amazon.aocagent.installers.otinstallers.OTInstaller;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.testbeds.TestBed;
import com.amazon.aocagent.validators.BatchedValidator;
import com.amazon.aocagent.validators.IValidator;

import java.util.List;

public class IntegTest implements ITask {
  TestBed testBed;
  OTInstaller otInstaller;
  List<OTEmitterInstaller> otEmitterInstallerList;
  BatchedValidator batchedValidator;
  Context context;

  /**
   * Construct IntegTest Object.
   *
   * @param testBed the testbed, for example: EC2
   * @param otInstaller the installer for ot package
   * @param otEmitterInstallerList the installers for the emitter image
   * @param validatorList the validator list
   */
  public IntegTest(
      TestBed testBed,
      OTInstaller otInstaller,
      List<OTEmitterInstaller> otEmitterInstallerList,
      List<IValidator> validatorList) {
    this.testBed = testBed;
    this.otInstaller = otInstaller;
    this.otEmitterInstallerList = otEmitterInstallerList;
    this.batchedValidator = new BatchedValidator(validatorList);
  }

  @Override
  public void init(Context context) throws Exception {
    testBed.init(context);
  }

  @Override
  public void execute() throws Exception {
    context = testBed.launchTestBed();

    if (otInstaller != null) {
      otInstaller.init(context);
      otInstaller.installAndStart();
    }

    for (OTEmitterInstaller emitterInstaller : this.otEmitterInstallerList) {
      emitterInstaller.init(context);
      emitterInstaller.installAndStart();
    }

    batchedValidator.init(context);
    batchedValidator.validate();
  }

  @Override
  public void clean() throws Exception {
    if (context.getEksTestArtifactsDir() != null) {
      context.getEksTestArtifactsDir().deletePath();
    }
  }
}
