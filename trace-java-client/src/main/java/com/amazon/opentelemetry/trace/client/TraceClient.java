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

package com.amazon.opentelemetry.trace.client;

import com.amazon.opentelemetry.trace.model.topology.LoadGeneratorParams;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public interface TraceClient {

  String emit() throws Exception;

  default LoadGeneratorParams initGenerator() throws Exception {
    try (InputStream inputStream = TraceClient.class.getResourceAsStream("/sample_trace.json");
          BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
      String json = reader.lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Gson gson = new Gson();
      return gson.fromJson(json, LoadGeneratorParams.class);
    } catch(Exception e) {
      System.out.println(e);
      throw e;
    }
  }

  default String normalizeUrl(String collectorUrl) {
    if (!collectorUrl.trim().toLowerCase().startsWith("http://")) {
      return "http://" + collectorUrl;
    }
    return collectorUrl;
  }

}
