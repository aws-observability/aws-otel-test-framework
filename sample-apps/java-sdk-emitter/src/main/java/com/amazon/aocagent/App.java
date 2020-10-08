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

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.TracingContextUtils;
import java.util.Random;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static spark.Spark.*;

public class App {
  static final String REQUEST_START_TIME = "requestStartTime";

  private static MetricEmitter buildMetricEmitter(){
    return new MetricEmitter();
  }


  public static void main(String[] args) {
    MetricEmitter metricEmitter = buildMetricEmitter();

    get("/", (req, res) -> {
      return "healthcheck";
    });

    get("/span0", (req, res) -> {
      Span currentSpan = TracingContextUtils.getCurrentSpan();
      List<String> spanList = new ArrayList<>();
      spanList.add(currentSpan.getContext().getSpanIdAsHexString());

      String spans = makeHttpCall("http://localhost:4567/span1");
      for(String spanId: spans.split(",")){
        spanList.add(spanId);
      }

      String traceId = currentSpan.getContext().getTraceIdAsHexString();
      String xrayTraceId = "1-" + traceId.substring(0, 8)
          + "-" + traceId.substring(8);

      Response response = new Response(xrayTraceId, spanList);

      return response;
    }, new JsonTransformer());

    get("/span400", (req, res) -> {
      res.status(400);
      return "params error";
    });

    get("/span500", (req, res) -> {
      res.status(500);
      return "internal error";
    });

    get("/span1", (req, res) -> {
      String nextSpanId = makeHttpCall("http://localhost:4567/span2");
      return TracingContextUtils.getCurrentSpan().getContext().getSpanIdAsHexString() + "," + nextSpanId;
    });

    get("/span2", (req, res) -> {
      return TracingContextUtils.getCurrentSpan().getContext().getSpanIdAsHexString();
    });

    /**
     * record a start time for each request
     */
    before((req, res) -> {
      req.attribute(REQUEST_START_TIME, System.currentTimeMillis());
    });

    after((req, res) -> {
      String statusCode = String.valueOf(res.status());
      // calculate return time
      Long requestStartTime = req.attribute(REQUEST_START_TIME);
      metricEmitter.emitReturnTimeMetric(
          System.currentTimeMillis()- requestStartTime,
          req.pathInfo(),
          statusCode
      );

      // emit http request load size
      metricEmitter.emitBytesSentMetric(
          req.contentLength() + mimicPayloadSize(),
          req.pathInfo(),
          statusCode);
    });

    exception(Exception.class, (exception, request, response) -> {
      // Handle the exception here
      exception.printStackTrace();
    });
  }

  private static String makeHttpCall(String url) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();
    try {
      HttpGet httpget = new HttpGet(url);

      System.out.println("Executing request " + httpget.getRequestLine());

      // Create a custom response handler
      ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

        @Override
        public String handleResponse(
            final HttpResponse response) throws ClientProtocolException, IOException {
          int status = response.getStatusLine().getStatusCode();
          if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
          } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
          }
        }

      };
      String responseBody = httpclient.execute(httpget, responseHandler);
      System.out.println("----------------------------------------");
      System.out.println(responseBody);
      return responseBody;
    } finally {
      httpclient.close();
    }
  }

  private static int mimicPayloadSize() {
    Random randomGenerator = new Random();
    return randomGenerator.nextInt(1000);
  }

}
