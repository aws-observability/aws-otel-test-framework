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

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.enums.GenericConstants;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.helpers.SortUtils;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.SampleAppResponse;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.models.xray.Entity;
import com.amazon.aoc.services.XRayService;
import com.amazonaws.services.xray.model.Segment;
import com.amazonaws.services.xray.model.Trace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.wnameless.json.flattener.JsonFlattener;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class TraceValidator implements IValidator {
  private MustacheHelper mustacheHelper = new MustacheHelper();
  private XRayService xrayService;
  private ICaller caller;
  private Context context;
  private FileConfig expectedTrace;

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

  @Override
  public void init(
      Context context, ValidationConfig validationConfig, ICaller caller, FileConfig expectedTrace)
      throws Exception {
    this.xrayService = new XRayService(context.getRegion());
    this.caller = caller;
    this.context = context;
    this.expectedTrace = expectedTrace;
  }

  @Override
  public void validate() throws Exception {
    // get stored trace
    Map<String, Object> storedTrace = this.getStoredTrace();
    log.info("value of stored trace map: {}", storedTrace);
    // create trace id list to retrieve trace from x-ray service
    String traceId = (String) storedTrace.get("[0].trace_id");
    List<String> traceIdList = Collections.singletonList(traceId);

    // get retrieved trace from x-ray service
    boolean isMatched = RetryHelper.retry(10,
        Integer.parseInt(GenericConstants.SLEEP_IN_MILLISECONDS.getVal()),
        false,
        () -> {
        Map<String, Object> retrievedTrace = this.getRetrievedTrace(traceIdList);
        log.info("value of retrieved trace map: {}", retrievedTrace);
        // data model validation of other fields of segment document
        for (Map.Entry<String, Object> entry : storedTrace.entrySet()) {
          String targetKey = entry.getKey();
          if (retrievedTrace.get(targetKey) == null) {
            log.error("mis target data: {}", targetKey);
            throw new BaseException(ExceptionCode.DATA_MODEL_NOT_MATCHED);
          }
          if (!entry
                  .getValue()
                  .toString()
                  .equalsIgnoreCase(retrievedTrace.get(targetKey).toString())) {
            log.error("data model validation failed");
            log.info("mis matched data model field list");
            log.info("value of stored trace map: {}", entry.getValue());
            log.info("value of retrieved map: {}", retrievedTrace.get(entry.getKey()));
            log.info("==========================================");
            throw new BaseException(ExceptionCode.DATA_MODEL_NOT_MATCHED);
          }
        }
      });

    if (!isMatched) {
      throw new BaseException(ExceptionCode.DATA_MODEL_NOT_MATCHED);
    }

    log.info("validation is passed for path {}", caller.getCallingPath());
  }

  // this method will hit get trace from x-ray service and get retrieved trace
  private Map<String, Object> getRetrievedTrace(List<String> traceIdList) throws Exception {
    AtomicReference<Map<String, Object>> flattenedJsonMapForRetrievedTrace =
        new AtomicReference<>();
    RetryHelper.retry(
        30,
        () -> {
          List<Trace> retrieveTraceList = null;
          retrieveTraceList = xrayService.listTraceByIds(traceIdList);
          if (retrieveTraceList == null || retrieveTraceList.isEmpty()) {
            throw new BaseException(ExceptionCode.EMPTY_LIST);
          }

          // in case the json format is wrong, retry it.
          if (!retrieveTraceList.isEmpty()) {
            flattenedJsonMapForRetrievedTrace.set(
                this.flattenDocument(retrieveTraceList.get(0).getSegments()));
          } else {
            log.error("retrieved trace list is empty or null");
            throw new BaseException(ExceptionCode.EMPTY_LIST);
          }
        });

    return flattenedJsonMapForRetrievedTrace.get();
  }

  private Map<String, Object> flattenDocument(List<Segment> segmentList) {
    List<Entity> entityList = new ArrayList<>();

    // Parse retrieved segment documents into a barebones Entity POJO
    for (Segment segment : segmentList) {
      Entity entity;
      try {
        entity = MAPPER.readValue(segment.getDocument(), Entity.class);
        entityList.add(entity);
      } catch (JsonProcessingException e) {
        log.warn("Error parsing segment JSON", e);
      }
    }

    // Recursively sort all segments and subsegments so the ordering is always consistent
    SortUtils.recursiveEntitySort(entityList);
    StringBuilder segmentsJson = new StringBuilder("[");

    // build the segment's document as a json array and flatten it for easy comparison
    for (Entity entity : entityList) {
      try {
        segmentsJson.append(MAPPER.writeValueAsString(entity));
        segmentsJson.append(",");
      } catch (JsonProcessingException e) {
        log.warn("Error serializing segment JSON", e);
      }
    }

    segmentsJson.replace(segmentsJson.length() - 1, segmentsJson.length(), "]");
    return JsonFlattener.flattenAsMap(segmentsJson.toString());
  }

  // this method will hit a http endpoints of sample web apps and get stored trace
  private Map<String, Object> getStoredTrace() throws Exception {
    Map<String, Object> flattenedJsonMapForStoredTraces = null;

    SampleAppResponse sampleAppResponse = this.caller.callSampleApp();

    String jsonExpectedTrace = mustacheHelper.render(this.expectedTrace, context);

    try {
      // flattened JSON object to a map
      flattenedJsonMapForStoredTraces = JsonFlattener.flattenAsMap(jsonExpectedTrace);
      flattenedJsonMapForStoredTraces.put("[0].trace_id", sampleAppResponse.getTraceId());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return flattenedJsonMapForStoredTraces;
  }
}
