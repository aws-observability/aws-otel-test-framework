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

package com.amazon.opentelemetry.trace.model.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Trace {

  public Span rootSpan;
  public List<Span> spans = new ArrayList<>();
  public Map<UUID, Span> spanIdToSpan = new HashMap<>();
  public Map<UUID, List<Reference>> spanIdToOutgoingRefs = new HashMap<>();

  public void addSpan(Span span) {
    this.spans.add(span);
    this.spanIdToSpan.put(span.id, span);
  }

  public void addRefs() {
    for (Span span : spans) {
      for (Reference ref : span.refs) {
        this.spanIdToOutgoingRefs.computeIfAbsent(ref.fromSpanId, id -> new ArrayList<>())
            .add(ref);
      }
    }
  }
}
