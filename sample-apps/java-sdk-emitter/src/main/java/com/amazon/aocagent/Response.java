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
package com.amazon.aocagent;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class Response {
  private String traceId;
  private List<String> spanIdList;

  public Response(String traceId, List<String> spanIdList) {
    this.traceId = traceId;
    this.spanIdList = spanIdList;
  }

  public String toJson() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper(new JsonFactory());
    return mapper.writeValueAsString(this);

  }

  public String getTraceId(){
    return traceId;
  }

  public List<String> getSpanIdList(){
    return spanIdList;
  }


}
