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

import com.amazon.aocagent.enums.TestCase;
import com.amazon.aocagent.models.Context;

public class IntegTestFactory {

  /**
   * run the testcase.
   *
   * @param testCase the testcase to run
   * @param context the testing context
   * @throws Exception when the test fails
   */
  public static void runTestCase(TestCase testCase, Context context) throws Exception {
    IntegTest integTest =
        new IntegTest(
            testCase.getTestBed(),
            testCase.getOtInstaller(),
            testCase.getOtEmitterInstallerList(),
            testCase.getValidatorList());

    try {
      integTest.init(context);
      integTest.execute();
    } finally {
      integTest.clean();
    }
  }
}
