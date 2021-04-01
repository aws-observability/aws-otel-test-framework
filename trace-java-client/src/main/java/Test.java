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

import com.amazon.opentelemetry.trace.client.JaegerTraceEmitClient;
import com.amazon.opentelemetry.trace.client.ZipkinTraceEmitClient;

public class Test {

  public static void main(String[] args) throws Exception {

//    emitJaegerTraces();

    emitZipkinTraces();
  }

  public static void emitJaegerTraces() throws Exception {
    JaegerTraceEmitClient jaegerClient = new JaegerTraceEmitClient("http://localhost:14268");
    for (;;) {
      String tracdId = jaegerClient.emit();
      System.out.println("jaeger traceid:" + tracdId);
      Thread.sleep(1000);
    }
  }

  public static void emitZipkinTraces() throws Exception {
    ZipkinTraceEmitClient zipkinClient = new ZipkinTraceEmitClient("http://localhost:9411");
    for (;;) {
      String tracdId = zipkinClient.emit();
      System.out.println("zipkin traceid:" + tracdId);
      Thread.sleep(1000);
    }
  }

}
