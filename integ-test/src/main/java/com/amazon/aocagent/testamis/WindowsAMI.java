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

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.enums.OSType;
import com.amazon.aocagent.enums.S3Package;
import com.amazonaws.services.ec2.model.InstanceType;

public class WindowsAMI implements ITestAMI {
  private String amiId;
  private boolean useSSM;

  public WindowsAMI(String amiId) {
    this.amiId = amiId;
    this.useSSM = true;
  }

  @Override
  public String getAMIId() {
    return this.amiId;
  }

  @Override
  public boolean isUseSSM() {
    return useSSM;
  }

  // getLoginUser() is not used in WindowsAMI, simply return null here
  @Override
  public String getLoginUser() {
    return null;
  }

  @Override
  public S3Package getS3Package() {
    return S3Package.WINDOWS_AMD64_MSI;
  }

  @Override
  public String getDownloadingCommand(String fromUrl, String toLocation) {
    return null;
  }

  @Override
  public String getInstallingCommand(String packagePath) {
    return null;
  }

  @Override
  public String getConfiguringCommand(String configContent) {
    return null;
  }

  @Override
  public String getStartingCommand(String configPath) {
    return null;
  }

  @Override
  public String getSsmDownloadingCommand(String fromUrl, String toLocation) {
    return String.format("wget %s -outfile C:\\%s", fromUrl, toLocation);
  }

  @Override
  public String getSsmInstallingCommand(String packagePath) {
    return String.format("msiexec /i C:\\%s", packagePath);
  }

  @Override
  public String getSsmConfiguringCommand(String configContent) {
    return String.format(
            "Set-Content -Path %s -Value \"%s\"\n",
            GenericConstants.EC2_WIN_CONFIG_PATH.getVal(), configContent);
  }

  @Override
  public String getSsmStartingCommand() {
    return String.format(
        "& %s -ConfigLocation %s -Action start",
        GenericConstants.WINDOWS_CTL_PATH.getVal(), GenericConstants.EC2_WIN_CONFIG_PATH.getVal());
  }

  @Override
  public String getDisableFirewallCommand() {
    return "Set-NetFirewallProfile -Profile Public -Enabled False";
  }

  @Override
  public String getSSMDocument() {
    return GenericConstants.RUN_POWER_SHELL_SCRIPT_DOCUMENT.getVal();
  }

  @Override
  public InstanceType getInstanceType() {
    return InstanceType.T2Medium;
  }
}
