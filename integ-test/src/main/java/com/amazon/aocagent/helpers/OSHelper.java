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

package com.amazon.aocagent.helpers;

public class OSHelper {
  private static String OS = System.getProperty("os.name").toLowerCase();

  /**
   * check whether current OS is Windows.
   *
   * @return current OS is Windows or not
   */
  public static boolean isWindows() {
    return OS.startsWith("win");
  }

  /**
   * check whether current OS is MacOS X.
   *
   * @return current OS is MacOS X or not
   */
  public static boolean isMac() {
    return OS.startsWith("mac");
  }

  /**
   * check whether current OS is Linux.
   *
   * @return current OS is Linux or not
   */
  public static boolean isLinux() {
    return OS.startsWith("linux") || OS.startsWith("ubuntu");
  }
}
