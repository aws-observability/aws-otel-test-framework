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
package com.amazon.aocagent.installers.otinstallers;

import com.amazon.aocagent.models.Context;

public interface OTInstaller {

  /**
   * Init context variables.
   * @param context test context
   * @throws Exception init exception
   */
  void init(Context context) throws Exception;

  /**
   * setup integration tests resources and execute the test cases.
   * @throws Exception setup exception
   */
  void installAndStart() throws Exception;
}
