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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

// Can generate about 16M different unique combinations
class TagNameGenerator {

  private static List<String> pokemon = new BufferedReader(
      new InputStreamReader(TagNameGenerator.class.getResourceAsStream("/pokemon.txt"))
  ).lines().collect(Collectors.toList());

  private static List<String> natures = new BufferedReader(
      new InputStreamReader(TagNameGenerator.class.getResourceAsStream("/natures.txt"))
  ).lines().collect(Collectors.toList());

  private static List<String> adjectives = new BufferedReader(
      new InputStreamReader(TagNameGenerator.class.getResourceAsStream("/adjectives.txt"))
  ).lines().collect(Collectors.toList());

  String getForIndex(int index) {
    return adjectives.get((adjectives.size() - 1) % (index + 1))
        + "-" + natures.get((natures.size() - 1) % (index + 1))
        + "-" + pokemon.get((pokemon.size() - 1) % (index + 1));
  }
}
