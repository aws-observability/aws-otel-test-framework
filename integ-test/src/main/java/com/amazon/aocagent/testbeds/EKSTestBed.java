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

package com.amazon.aocagent.testbeds;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.helpers.EKSTestOptionsValidationHelper;
import com.amazon.aocagent.helpers.TempDirHelper;
import com.amazon.aocagent.models.Context;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EKSTestBed implements TestBed {
  private Context context;

  @Override
  public void init(Context context) {
    this.context = context;
  }

  @Override
  public Context launchTestBed() throws Exception {
    context.setEksTestArtifactsDir(new TempDirHelper(GenericConstants.EKS_INTEG_TEST.getVal()));
    EKSTestOptionsValidationHelper.checkEKSTestOptions(context);
    return this.context;
  }
}
