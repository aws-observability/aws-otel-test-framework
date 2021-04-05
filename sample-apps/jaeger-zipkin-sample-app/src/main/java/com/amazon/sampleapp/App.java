/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.amazon.sampleapp;

import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.ipAddress;
import static spark.Spark.port;

import com.amazon.opentelemetry.trace.client.JaegerTraceEmitClient;
import com.amazon.opentelemetry.trace.client.TraceClient;
import com.amazon.opentelemetry.trace.client.ZipkinTraceEmitClient;
import java.io.IOException;
import java.io.UncheckedIOException;
import okhttp3.Call;
import okhttp3.OkHttpClient;

public class App {

  public static void main(String[] args) throws Exception {

    // initialize Jaeger trace emitter client
    String collectorUrl = System.getenv("JAEGER_RECEIVER_ENDPOINT");
    if (collectorUrl == null) {
      collectorUrl = "0.0.0.0:14268";
    }
    TraceClient jaegerClient = new JaegerTraceEmitClient(collectorUrl);

    // initialize Jaeger trace emitter client
    collectorUrl = System.getenv("ZIPKIN_RECEIVER_ENDPOINT");
    if (collectorUrl == null) {
      collectorUrl = "0.0.0.0:9411";
    }
    TraceClient zipkinClient = new ZipkinTraceEmitClient(collectorUrl);

    final Call.Factory httpClient = new OkHttpClient();
    String port;
    String host;
    String listenAddress = System.getenv("LISTEN_ADDRESS");

    if (listenAddress == null) {
      host = "127.0.0.1";
      port = "4567";
    } else {
      String[] splitAddress = listenAddress.split(":");
      host = splitAddress[0];
      port = splitAddress[1];
    }

    // set sampleapp app port number and ip address
    port(Integer.parseInt(port));
    ipAddress(host);

    get(
        "/",
        (req, res) -> {
          return "healthcheck";
        });

    /** zipkin trace request */
    get(
        "/outgoing-zipkin-http-call",
        (req, res) -> {
          String traceId;
          try {
            traceId = zipkinClient.emit();
          } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch endpoint", e);
          }

          return String.format("{\"traceId\": \"%s\"}", traceId);
        });

    /** jaeger trace request */
    get(
        "/outgoing-jaeger-http-call",
        (req, res) -> {
          String traceId;
          try {
            traceId = jaegerClient.emit();
          } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch endpoint", e);
          }

          return String.format("{\"traceId\": \"%s\"}", traceId);
        });

    exception(
        Exception.class,
        (exception, request, response) -> {
          // Handle the exception here
          exception.printStackTrace();
        });
  }

}
