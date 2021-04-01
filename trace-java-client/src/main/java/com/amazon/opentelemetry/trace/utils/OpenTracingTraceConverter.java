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

import com.amazon.opentelemetry.trace.model.trace.KeyValue;
import com.amazon.opentelemetry.trace.model.trace.Span;
import io.opentracing.Tracer;
import java.util.Map;
import java.util.UUID;

public final class OpenTracingTraceConverter {

    private OpenTracingTraceConverter() {}

    public static io.opentracing.Span createOTSpan(
        Tracer tracer, Span span, Map<UUID, io.opentracing.Span> parentSpans
    ) {
        Tracer.SpanBuilder otSpanBuilder = tracer.buildSpan(span.operationName)
                .withStartTimestamp(span.startTimeMicros);
        for (KeyValue tag : span.tags) {
            otSpanBuilder = addModelTag(tag, otSpanBuilder);
        }
        final Tracer.SpanBuilder finalSpanBuilder = otSpanBuilder;
        span.refs.forEach(ref -> {
            switch (ref.refType) {
                case CHILD_OF:
                    finalSpanBuilder.addReference(
                            io.opentracing.References.CHILD_OF,
                            parentSpans.get(ref.fromSpanId).context()
                    );
                    break;
                case FOLLOWS_FROM:
                    finalSpanBuilder.addReference(
                            io.opentracing.References.FOLLOWS_FROM,
                            parentSpans.get(ref.fromSpanId).context()
                    );
                    break;
                default:
                    break;
            }
        });
        return finalSpanBuilder.start();
    }

    private static Tracer.SpanBuilder addModelTag(KeyValue tag, Tracer.SpanBuilder otSpanBld) {
        if (tag.valueType.equalsIgnoreCase(KeyValue.STRING_VALUE_TYPE)) {
            otSpanBld = otSpanBld.withTag(tag.key, tag.valueString);
        } else if (tag.valueType.equalsIgnoreCase(KeyValue.BOOLEAN_VALUE_TYPE)) {
            otSpanBld = otSpanBld.withTag(tag.key, tag.valueBool);
        } else if (tag.valueType.equalsIgnoreCase(KeyValue.LONG_VALUE_TYPE)) {
            otSpanBld = otSpanBld.withTag(tag.key, tag.valueLong);
        } // other types are ignored for now
        return otSpanBld;
    }
}
