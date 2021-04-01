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

package com.amazon.opentelemetry.trace.utils;

import com.amazon.opentelemetry.trace.model.trace.Reference;
import com.amazon.opentelemetry.trace.model.trace.Span;
import com.amazon.opentelemetry.trace.model.trace.Trace;
import java.util.List;
import java.util.function.Consumer;

public class TraceTraversal {
    public static void prePostOrder(Trace trace, Consumer<Span> preVisitConsumer, Consumer<Span> postVisitConsumer) {
        prePostOrder(trace, trace.rootSpan, preVisitConsumer, postVisitConsumer);
    }

    private static void prePostOrder(
        Trace trace,
        Span span,
        Consumer<Span> preVisitConsumer,
        Consumer<Span> postVisitConsumer
    ) {
        preVisitConsumer.accept(span);
        List<Reference> outgoing = trace.spanIdToOutgoingRefs.get(span.id);
        if (outgoing != null) {
            outgoing.stream()
            .map(ref -> trace.spanIdToSpan.get(ref.toSpanId))
            .forEach(descendant -> prePostOrder(trace, descendant, preVisitConsumer, postVisitConsumer));
        }
        postVisitConsumer.accept(span);
    }
}
