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

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum LocalPackage {
  LINUX_AMD64_RPM(OSType.LINUX, Architecture.AMD64, PackageType.RPM),
  LINUX_ARM64_RPM(OSType.LINUX, Architecture.ARM64, PackageType.RPM),
  DEBIAN_AMD64_DEB(OSType.DEBIAN, Architecture.AMD64, PackageType.DEB),
  DEBIAN_ARM64_DEB(OSType.DEBIAN, Architecture.ARM64, PackageType.DEB),
  WINDOWS_AMD64_MSI(OSType.WINDOWS, Architecture.AMD64, PackageType.MSI),
  ;

  private PackageType packageType;
  private Architecture architecture;
  private OSType osType;

  LocalPackage(OSType osType, Architecture architecture, PackageType packageType) {
    this.osType = osType;
    this.architecture = architecture;
    this.packageType = packageType;
  }

  /**
   * getFilePath generates the local path for the package.
   *
   * @param localPackagesDir is used as the "root" directory of the package
   * @return the local path of the package
   */
  public String getFilePath(String localPackagesDir) {
    return String.join(
        "/",
        Arrays.asList(
            localPackagesDir,
            osType.name().toLowerCase(),
            architecture.name().toLowerCase(),
            GenericConstants.PACKAGE_NAME_PREFIX.getVal() + packageType.name().toLowerCase()));
  }
}
