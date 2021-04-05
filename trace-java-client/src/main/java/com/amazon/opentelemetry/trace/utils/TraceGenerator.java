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

import com.amazon.opentelemetry.trace.model.topology.LoadGeneratorParams;
import com.amazon.opentelemetry.trace.model.topology.ServiceRoute;
import com.amazon.opentelemetry.trace.model.topology.ServiceTier;
import com.amazon.opentelemetry.trace.model.topology.TagGenerator;
import com.amazon.opentelemetry.trace.model.topology.TagSet;
import com.amazon.opentelemetry.trace.model.topology.Topology;
import com.amazon.opentelemetry.trace.model.trace.KeyValue;
import com.amazon.opentelemetry.trace.model.trace.Reference;
import com.amazon.opentelemetry.trace.model.trace.Reference.RefType;
import com.amazon.opentelemetry.trace.model.trace.Service;
import com.amazon.opentelemetry.trace.model.trace.Span;
import com.amazon.opentelemetry.trace.model.trace.Trace;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TraceGenerator {
    private final Random random = new Random();
    private final Trace trace = new Trace();
    private Topology topology;

    private static final AtomicLong sequenceNumber = new AtomicLong(1);

    public static Trace generate(LoadGeneratorParams params, long startTimeMicros) {
        TraceGenerator gen = new TraceGenerator(params.topology);
        ServiceTier rootService = gen.topology.getServiceTier(params.rootRoute.service);
        Span rootSpan = gen.createSpanForServiceRouteCall(null, rootService, params.rootRoute.route, startTimeMicros);
        gen.trace.rootSpan = rootSpan;
        gen.trace.addRefs();
        return gen.trace;
    }

    private TraceGenerator(Topology topology) {
        this.topology = topology;
    }

    private Span createSpanForServiceRouteCall(TagSet parentTagSet, ServiceTier serviceTier, String routeName, long startTimeMicros) {
        String instanceName = serviceTier.instances.get(
                random.nextInt(serviceTier.instances.size()));
        ServiceRoute route = serviceTier.getRoute(routeName);

        // send tags of serviceTier and serviceTier instance
        Service service = new Service(serviceTier.serviceName, instanceName, new ArrayList<>());
        Span span = new Span();
        span.startTimeMicros = startTimeMicros;
        span.operationName = route.route;
        span.service = service;
        span.tags.add(KeyValue.ofLongType("load_generator.seq_num", sequenceNumber.getAndIncrement()));

        // Setup base tags
        span.setHttpMethodTag("GET");
        span.setHttpUrlTag("http://" + serviceTier.serviceName + routeName);
        // Get additional tags for this route, and update with any inherited tags
        TagSet routeTags = serviceTier.getTagSet(routeName);
        HashMap<String, Object> tagsToSet = new HashMap<>(routeTags.tags);
        for (TagGenerator tagGenerator : routeTags.tagGenerators) {
            tagsToSet.putAll(tagGenerator.generateTags());
        }
        if (parentTagSet != null && routeTags.inherit != null) {
            for (String inheritTagKey : routeTags.inherit) {
                Object value = parentTagSet.tags.get(inheritTagKey);
                if (value != null) {
                    tagsToSet.put(inheritTagKey, value);
                }
            }
        }

        // Set the additional tags on the span
        List<KeyValue> spanTags = tagsToSet.entrySet().stream()
            .map(t -> {
                Object val = t.getValue();
                if (val instanceof String) {
                    return KeyValue.ofStringType(t.getKey(), (String) val);
                }
                if (val instanceof Double) {
                    return KeyValue.ofLongType(t.getKey(), ((Double) val).longValue());
                }
                if (val instanceof Boolean) {
                    return KeyValue.ofBooleanType(t.getKey(), (Boolean) val);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        span.tags.addAll(spanTags);

        final AtomicLong maxEndTime = new AtomicLong(startTimeMicros);
        if (span.isErrorSpan()) {
            // inject root cause error and terminate trace there
            span.markRootCauseError();
        } else {
            // no error, make downstream calls
            route.downstreamCalls.forEach((s, r) -> {
                long childStartTimeMicros = startTimeMicros + TimeUnit.MILLISECONDS.toMicros(random.nextInt(route.maxLatencyMillis));
                ServiceTier childSvc = this.topology.getServiceTier(s);
                Span childSpan = createSpanForServiceRouteCall(routeTags, childSvc, r, childStartTimeMicros);
                Reference ref = new Reference(RefType.CHILD_OF, span.id, childSpan.id);
                childSpan.refs.add(ref);
                maxEndTime.set(Math.max(maxEndTime.get(), childSpan.endTimeMicros));
                if (childSpan.isErrorSpan()) {
                    Integer httpCode = childSpan.getHttpCode();
                    if (httpCode != null) {
                        span.setHttpCode(httpCode);
                    }
                    span.markError();
                }
            });
        }
        long ownDuration = TimeUnit.MILLISECONDS.toMicros((long)this.random.nextInt(route.maxLatencyMillis));
        span.endTimeMicros = maxEndTime.get() + ownDuration;
        trace.addSpan(span);
        return span;
    }
}
