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

package com.amazon.aoc.validators;

import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.TraceFromEmitter;
import com.amazon.aoc.services.S3Service;
import com.amazon.aoc.services.XRayService;
import com.amazonaws.services.xray.model.Segment;
import com.amazonaws.services.xray.model.Trace;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Log4j2
public class TraceValidator implements IValidator {
  private MustacheHelper mustacheHelper = new MustacheHelper();
  private static int MAX_RETRY_COUNT = 60;
  private Context context;
  private XRayService xrayService;
  private S3Service s3Service;

  @Override
  public void init(Context context) throws Exception {
    this.context = context;
    this.xrayService = new XRayService(context.getRegion());
    this.s3Service = new S3Service(context.getRegion());
  }

  @Override
  public void validate() throws Exception {
    List<Trace> expectedTraceList = this.getExpectedTrace();
    expectedTraceList.sort(Comparator.comparing(Trace::getId));
    RetryHelper.retry(
        MAX_RETRY_COUNT,
        () -> {
          List<Trace> traceList =
              xrayService.listTraceByIds(
                  expectedTraceList.stream()
                      .map(trace -> trace.getId())
                      .collect(Collectors.toList()));

          traceList.sort(Comparator.comparing(Trace::getId));

          log.info("expectedTraceList: {}", expectedTraceList);
          log.info("traceList got from backend: {}", traceList);
          if (expectedTraceList.size() != traceList.size()) {
            throw new BaseException(ExceptionCode.TRACE_LIST_NOT_MATCHED);
          }

          for (int i = 0; i != expectedTraceList.size(); ++i) {
            // remove the s3 span as the auto-instrumenting of s3 happens before we store trace data
            // onto s3.
            Trace trace = traceList.get(i);
            trace.getSegments().removeIf(span -> span.getDocument().contains("AWS::S3"));
            compareTwoTraces(expectedTraceList.get(i), trace);
          }
        });
  }

  private void compareTwoTraces(Trace trace1, Trace trace2) throws BaseException {
    // check trace id
    if (!trace1.getId().equals(trace2.getId())) {
      throw new BaseException(ExceptionCode.TRACE_ID_NOT_MATCHED);
    }

    if (trace1.getSegments().size() != trace2.getSegments().size()) {
      throw new BaseException(ExceptionCode.TRACE_SPAN_LIST_NOT_MATCHED);
    }
    trace1.getSegments().sort(Comparator.comparing(Segment::getId));
    trace2.getSegments().sort(Comparator.comparing(Segment::getId));

    for (int i = 0; i != trace1.getSegments().size(); ++i) {
      // check span id
      if (!trace1.getSegments().get(i).getId()
          .equals(trace2.getSegments().get(i).getId())) {
        throw new BaseException(ExceptionCode.TRACE_SPAN_NOT_MATCHED);
      }
    }
  }

  // this endpoint will be a http endpoint including the path with get method
  private List<Trace> getExpectedTrace() throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder()
      .url(context.getDataEmitterEndpoint())
      .build();

    AtomicReference<String> responseContent = new AtomicReference<>();
    RetryHelper.retry(()->{
        try(Response response = client.newCall(request).execute()){
          if(!response.isSuccessful()){
            throw new BaseException(ExceptionCode.DATA_EMITTER_UNAVAILABLE);
          }
          responseContent.set(response.body().string());
        }
      }
    );

    TraceFromEmitter traceFromEmitter = new ObjectMapper().readValue(responseContent.get(), TraceFromEmitter.class);

    // convert the trace data into xray format
    Trace trace = new Trace();
    trace.setId(traceFromEmitter.getTraceId());

    List<Segment> segments = new ArrayList<>();
    for(String spanId: traceFromEmitter.getSpanIdList()){
      segments.add(new Segment().withId(spanId));
    }
    trace.setSegments(segments);

    // we can support multi expected trace id to validate
    return Arrays.asList(trace);
  }
}
