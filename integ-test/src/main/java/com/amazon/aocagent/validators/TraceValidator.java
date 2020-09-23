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
package com.amazon.aocagent.validators;

import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.helpers.MustacheHelper;
import com.amazon.aocagent.helpers.RetryHelper;
import com.amazon.aocagent.models.Context;
import com.amazon.aocagent.models.TraceFromEmitter;
import com.amazon.aocagent.services.S3Service;
import com.amazon.aocagent.services.XRayService;
import com.amazonaws.services.xray.model.Segment;
import com.amazonaws.services.xray.model.Trace;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
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
    this.xrayService = new XRayService(context.getStack().getTestingRegion());
    this.s3Service = new S3Service(context.getStack().getTestingRegion());
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

  private List<Trace> getExpectedTrace() throws IOException {
    // get expected trace from s3
    String strTraceData =
        s3Service.getS3ObjectAsString(
            context.getStack().getTraceDataS3BucketName(),
            context.getInstanceId() // we use instanceid as the s3key
            );
    log.info("get expected trace data from s3: {}", strTraceData);

    // load traceData from json, we use json instead of yaml because storing yaml in s3 will lose
    // the spaces so that the yaml format will be invalid to read
    ObjectMapper mapper = new ObjectMapper(new JsonFactory());
    TraceFromEmitter traceFromEmitter =
        mapper.readValue(strTraceData.getBytes(StandardCharsets.UTF_8),
            new TypeReference<TraceFromEmitter>() {});

    // convert the trace data into xray format
    String yamlExpectedTrace = mustacheHelper.render(context.getExpectedTrace(), traceFromEmitter);

    // load xray trace from yaml
    mapper = new ObjectMapper(new YAMLFactory());
    List<Trace> expectedTraceList =
        mapper.readValue(
            yamlExpectedTrace.getBytes(StandardCharsets.UTF_8),
            new TypeReference<List<Trace>>() {});

    return expectedTraceList;
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
}
