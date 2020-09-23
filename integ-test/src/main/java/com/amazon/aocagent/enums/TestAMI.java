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

import com.amazon.aocagent.testamis.A1AmazonLinuxAMI;
import com.amazon.aocagent.testamis.A1RedHatAMI;
import com.amazon.aocagent.testamis.A1SuseAMI;
import com.amazon.aocagent.testamis.A1UbuntuAMI;
import com.amazon.aocagent.testamis.AmazonLinuxAMI;
import com.amazon.aocagent.testamis.Centos6AMI;
import com.amazon.aocagent.testamis.CentosAMI;
import com.amazon.aocagent.testamis.DebianAMI;
import com.amazon.aocagent.testamis.EcsOptimizedAMI;
import com.amazon.aocagent.testamis.ITestAMI;
import com.amazon.aocagent.testamis.RedHat6AMI;
import com.amazon.aocagent.testamis.RedHatAMI;
import com.amazon.aocagent.testamis.SuseAMI;
import com.amazon.aocagent.testamis.UbuntuAMI;
import com.amazon.aocagent.testamis.WindowsAMI;
import lombok.Getter;

@Getter
public enum TestAMI {
  // Amazonlinux
  AMAZON_LINUX(new AmazonLinuxAMI("ami-0a07be880014c7b8e")),
  AMAZON_LINUX2(new AmazonLinuxAMI("ami-0873b46c45c11058d")),
  A1_AMAZON_LINUX(new A1AmazonLinuxAMI("ami-091a6d6d0ed7b35fd")),

  // Suse
  SUSE_15(new SuseAMI("ami-063c2d222d223d0e9")),
  SUSE_12(new SuseAMI("ami-811794f9")),
  A1_SUSE_15(new A1SuseAMI("ami-0bfc92b18fd79372c")),

  // redhat
  REDHAT_8(new RedHatAMI("ami-079596bf7a949ddf8")),
  REDHAT_7(new RedHatAMI("ami-078a6a18fb73909b2")),
  REDHAT_6(new RedHat6AMI("ami-4dc28f7d")),
  A1_REDHAT_8(new A1RedHatAMI("ami-0f7a968a2c17fb48b")),
  A1_REDHAT_7(new A1RedHatAMI("ami-0e00026dd0f3688e2")),

  // centos
  //CENTOS_8(new CentosAMI("ami-91ea11f1")),
  CENTOS_7(new CentosAMI("ami-0bc06212a56393ee1")),
  CENTOS_6(new Centos6AMI("ami-052ff42ae3be02b6a")),

  // debian
  DEBIAN_10(new DebianAMI("ami-0bb8fb45872332e66")),
  DEBIAN_9(new DebianAMI("ami-0ccb963e85bc5c856")),
  //DEBIAN_8(new DebianAMI("ami-fde96b9d")),

  // ubuntu
  UBUNTU_18_04(new UbuntuAMI("ami-0edf3b95e26a682df")),
  UBUNTU_16_04(new UbuntuAMI("ami-6e1a0117")),
  UBUNTU_14_04(new UbuntuAMI("ami-718c6909")),
  A1_UBUNTU_18_04(new A1UbuntuAMI("ami-0db180c518750ee4f")),
  A1_UBUNTU_16_04(new A1UbuntuAMI("ami-05e1b2aec3b47890f")),

  //Windows
  WINDOWS_2019_BASE(new WindowsAMI("ami-09fa39d0afa9024db")),

  // ECS Optimized AMI
  ECS_OPTIMIZED(new EcsOptimizedAMI("ami-004e1655142a7ea0d")),
  ;

  private ITestAMI testAMIObj;

  TestAMI(ITestAMI testAMI) {
    this.testAMIObj = testAMI;
  }
}
