/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazon.opentelemetry.trace.model.topology;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;

public class TagGenerator {

  private Random rand = new Random();
  private TagNameGenerator tagGen = new TagNameGenerator();

  public int valLength = 10;
  public int numTags = 0;
  public int numVals = 0;

  public Map<String, Object> generateTags() {
    Map<String, Object> retVal = new HashMap<>();
    for (int genIndex = 0; genIndex < numTags; genIndex++) {
      String val;
      val = RandomStringUtils
          .random(valLength, 0, 0, true, true, null, new Random(rand.nextInt(numVals)));
      retVal.put(tagGen.getForIndex(genIndex), val);
    }

    return retVal;
  }
}
