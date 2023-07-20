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
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.helpers.SortUtils;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.models.xray.Entity;
import com.amazon.aoc.services.XRayService;
import com.amazonaws.services.xray.model.Segment;
import com.amazonaws.services.xray.model.Trace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.wnameless.json.flattener.JsonFlattener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class XrayValidator implements IValidator {
  public MustacheHelper mustacheHelper = new MustacheHelper();
  public XRayService xrayService;
  public ICaller caller;
  public Context context;
  public FileConfig expectedTrace;

  private static final ObjectMapper MAPPER =
      new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

  @Override
  public void init(
      Context context, ValidationConfig validationConfig, ICaller caller, FileConfig expectedTrace)
      throws Exception {
    this.xrayService = new XRayService(context.getRegion());
    this.caller = caller;
    this.context = context;
    this.expectedTrace = expectedTrace;
  }

  // this method will hit get trace from x-ray service and get retrieved trace
  public Map<String, Map<String, Object>> getActualTraces(List<String> traceIdList)
      throws Exception {
    List<Trace> actualTraceList = xrayService.listTraceByIds(traceIdList);
    if (actualTraceList == null || actualTraceList.isEmpty()) {
      throw new BaseException(ExceptionCode.EMPTY_LIST);
    }

    Map<String, Map<String, Object>> actualTraceListFlattened =
        new HashMap<String, Map<String, Object>>();
    for (Trace trace : actualTraceList) {
      actualTraceListFlattened.put(trace.getId(), this.flattenDocument(trace.getSegments()));
    }

    return actualTraceListFlattened;
  }

  // this method will hit get trace from x-ray service and get retrieved trace
  public Map<String, Object> getActualTrace(String traceId) throws Exception {
    Trace actualTrace = xrayService.getTraceById(traceId);
    if (actualTrace == null) {
      throw new BaseException(ExceptionCode.NULL_VAR);
    }

    return this.flattenDocument(actualTrace.getSegments());
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
}
