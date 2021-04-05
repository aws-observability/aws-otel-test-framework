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

public class SpanConventions {
    // Span tags as suggested by OpenTracing semantic conventions here:
    // https://github.com/opentracing/specification/blob/master/semantic_conventions.md
    public static String HTTP_METHOD_KEY = "http.method";
    public static String HTTP_STATUS_CODE_KEY = "http.status_code";
    public static String HTTP_URL_KEY = "http.url";
    public static String IS_ERROR_KEY = "error";

    // Omnition conventions
    public static String IS_ROOT_CAUSE_ERROR_KEY = "root_cause_error";
}
