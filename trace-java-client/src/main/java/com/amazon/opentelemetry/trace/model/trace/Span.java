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

import com.amazon.opentelemetry.trace.utils.SpanConventions;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Span {

  public final UUID id = UUID.randomUUID();
  public Service service;
  public Long startTimeMicros;
  public Long endTimeMicros;
  public String operationName;
  public List<KeyValue> tags = new ArrayList<>();
  public List<Reference> refs = new ArrayList<>();

  public void markError() {
    this.tags.add(KeyValue.ofBooleanType(SpanConventions.IS_ERROR_KEY, true));
  }

  public void markRootCauseError() {
    this.tags.add(KeyValue.ofBooleanType(SpanConventions.IS_ROOT_CAUSE_ERROR_KEY, true));
  }

  public boolean isErrorSpan() {
    return tags.stream()
        .anyMatch(kv ->
            (kv.key.equalsIgnoreCase(SpanConventions.HTTP_STATUS_CODE_KEY) && kv.valueLong != 200)
                || (kv.key.equalsIgnoreCase(SpanConventions.IS_ERROR_KEY) && kv.valueBool));
  }

  public void setHttpCode(int code) {
    this.tags.add(KeyValue.ofLongType(SpanConventions.HTTP_STATUS_CODE_KEY, (long) code));
  }

  public Integer getHttpCode() {
    return tags.stream()
        .filter(kv -> kv.key.equalsIgnoreCase(SpanConventions.HTTP_STATUS_CODE_KEY))
        .map(kv -> kv.valueLong.intValue())
        .findFirst().orElse(null);
  }

  public void setHttpUrlTag(String url) {
    this.tags.add(KeyValue.ofStringType(SpanConventions.HTTP_URL_KEY, url));
  }

  public void setHttpMethodTag(String method) {
    this.tags.add(KeyValue.ofStringType(SpanConventions.HTTP_METHOD_KEY, method));
  }
}
