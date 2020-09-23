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
package com.amazon.aocagent.validators;

import com.amazon.aocagent.models.Context;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class BatchedValidator {
  List<IValidator> validatorList;
  Context context;

  public BatchedValidator(List<IValidator> validatorList) {
    this.validatorList = validatorList;
  }

  /**
   * validate runs all the validators configured.
   *
   * @throws Exception when the validation fails
   */
  public void validate() throws Exception {
    for (IValidator validator : this.validatorList) {
      validator.init(context);
      validator.validate();
    }

    log.info("Validation is passed!!!");
  }

  public void init(Context context) {
    this.context = context;
  }
}
