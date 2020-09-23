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

package com.amazon.aocagent.enums;

import com.amazon.aocagent.installers.emiterinstallers.OTEmitterInstaller;
import com.amazon.aocagent.installers.emiterinstallers.OTMetricAndTraceEmitterInstaller;
import com.amazon.aocagent.installers.otinstallers.ECSInstaller;
import com.amazon.aocagent.installers.otinstallers.EKSInstaller;
import com.amazon.aocagent.installers.otinstallers.OTInstaller;
import com.amazon.aocagent.installers.otinstallers.OTPackageInstaller;
import com.amazon.aocagent.installers.otinstallers.SsmOTPackageInstaller;
import com.amazon.aocagent.testbeds.EC2TestBed;
import com.amazon.aocagent.testbeds.ECSTestBed;
import com.amazon.aocagent.testbeds.EKSTestBed;
import com.amazon.aocagent.testbeds.TestBed;
import com.amazon.aocagent.validators.IValidator;
import com.amazon.aocagent.validators.MetricValidator;
import com.amazon.aocagent.validators.TraceValidator;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum TestCase {
  EC2_TEST(
      new EC2TestBed(),
      new OTPackageInstaller(),
      Arrays.asList(new OTMetricAndTraceEmitterInstaller()),
      Arrays.asList(new MetricValidator(), new TraceValidator())),

  EC2_SSM_TEST(
      new EC2TestBed(),
      new SsmOTPackageInstaller(),
      Arrays.asList(new OTMetricAndTraceEmitterInstaller()),
      Arrays.asList(new MetricValidator(), new TraceValidator())),

  // run AOC with data emitter in ECS as sidecar
  // tested both ECS fargate and EC2 modes
  ECS_TEST(
      new ECSTestBed(),
      new ECSInstaller(),
      Arrays.asList(), // data emitter is included in sidecar installer
      Arrays.asList(new MetricValidator(), new TraceValidator())),

  // run AOC with data emitter in EKS as sidecar
  EKS_TEST(
      new EKSTestBed(),
      new EKSInstaller(),
      Arrays.asList(), // data emitter is included in sidecar installer
      Arrays.asList(new MetricValidator(), new TraceValidator())),
  ;

  private TestBed testBed;
  private OTInstaller otInstaller;
  private List<OTEmitterInstaller> otEmitterInstallerList;
  private List<IValidator> validatorList;

  TestCase(
      TestBed testBed,
      OTInstaller otInstaller,
      List<OTEmitterInstaller> otEmitterInstallerList,
      List<IValidator> validatorList) {
    this.testBed = testBed;
    this.otInstaller = otInstaller;
    this.otEmitterInstallerList = otEmitterInstallerList;
    this.validatorList = validatorList;
  }
}
