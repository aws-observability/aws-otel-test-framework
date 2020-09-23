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

import com.amazon.aocagent.fileconfigs.FileConfig;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.models.Stack;
import org.junit.Test;

import java.io.IOException;

public class MustacheHelperTest {

  @Test
  public void testMustache() throws IOException {
    MustacheHelper mustacheHelper = new MustacheHelper();
    FileConfig fileConfig = new FileConfig() {
      @Override
      public String getPath() {
        return "/test.mustache";
      }
    };

    Context context = new Context();
    Stack stack = new Stack();
    stack.setTraceDataS3BucketName("test");
    context.setStack(stack);

    System.out.println(mustacheHelper.render(fileConfig, context));
  }
}
