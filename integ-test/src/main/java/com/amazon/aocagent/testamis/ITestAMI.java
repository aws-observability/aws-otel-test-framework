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

package com.amazon.aocagent.testamis;

import com.amazon.aocagent.enums.OSType;
import com.amazon.aocagent.enums.S3Package;
import com.amazonaws.services.ec2.model.InstanceType;

import java.util.List;

public interface ITestAMI {
  String getAMIId();

  boolean isUseSSM();

  String getLoginUser();

  S3Package getS3Package();

  String getDownloadingCommand(String fromUrl, String toLocation);

  String getInstallingCommand(String packagePath);

  String getConfiguringCommand(String configContent);

  String getStartingCommand(String configPath);

  String getDisableFirewallCommand();

  String getSsmDownloadingCommand(String fromUrl, String toLocation);

  String getSsmInstallingCommand(String packagePath);

  String getSsmConfiguringCommand(String configContent);

  String getSsmStartingCommand();

  String getSSMDocument();

  InstanceType getInstanceType();
}
