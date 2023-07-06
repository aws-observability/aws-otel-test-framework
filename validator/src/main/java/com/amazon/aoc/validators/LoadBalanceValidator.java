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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LoadBalanceValidator implements IValidator {
  private MustacheHelper mustacheHelper = new MustacheHelper();
  private XRayService xrayService;
  private ICaller caller;
  private Context context;

  private static final ObjectMapper MAPPER =
      new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

  // for unit test
  public void setXRayService(XRayService xrayService) {
    this.xrayService = xrayService;
  }

  @Override
  public void init(
      Context context, ValidationConfig validationConfig, ICaller caller, FileConfig expectedTrace)
      throws Exception {
    this.xrayService = new XRayService(context.getRegion());
    this.caller = caller;
    this.context = context;
  }

  @Override
  public void validate() throws Exception {
    // 2 retries for calling the sample app to handle the Lambda case,
    // where first request might be a cold start and have an additional unexpected subsegment
    int successes = 0;
    while (successes < 5) {
      // Call sample app and get locally stored trace
      String traceId = this.getAppTraceId();
      List<String> traceIdList = Collections.singletonList(traceId);
      ;
      log.info("value of Sample App traceId: {}", traceId);

      // Retry 5 times to since segments might not be immediately available in X-Ray service
      RetryHelper.retry(
          5,
          Integer.parseInt(GenericConstants.SLEEP_IN_MILLISECONDS.getVal()),
          true,
          () -> {
            // get retrieved trace from x-ray service
            Map<String, Object> retrievedTrace = this.getRetrievedTrace(traceIdList);
            log.info("value of retrieved trace map: {}", retrievedTrace);
            Set<String> set =
                retrievedTrace.keySet().stream()
                    .filter(s -> s.endsWith("collector-id"))
                    .collect(Collectors.toSet());
            if (set.size() < 2) {
              log.error("not enough spans in trace map (need at least 2)");
              log.info("==========================================");
              throw new BaseException(ExceptionCode.NOT_ENOUGH_SPANS);
            }
            String targetCollectorId = null;
            for (String collectorId : set) {
              if (targetCollectorId == null) {
                targetCollectorId = retrievedTrace.get(collectorId).toString();
              } else {
                if (!targetCollectorId.equals(retrievedTrace.get(collectorId).toString())) {
                  log.info("id values do not match");
                  log.info("value of target id: {}", targetCollectorId);
                  log.info(
                      "value of retrieved id: {}", retrievedTrace.get(collectorId).toString());
                  log.info("==========================================");
                  throw new BaseException(ExceptionCode.COLLECTOR_ID_NOT_MATCHED);
                }
              }
            }
          });
      
      successes += 1;
      log.info("Total number of successful runs: {}", successes);
    }

    log.info("validation is passed for path {}", caller.getCallingPath());
  }

  // this method will hit get trace from x-ray service and get retrieved trace
  private Map<String, Object> getRetrievedTrace(List<String> traceIdList) throws Exception {
    List<Trace> retrieveTraceList = xrayService.listTraceByIds(traceIdList);
    if (retrieveTraceList == null || retrieveTraceList.isEmpty()) {
      throw new BaseException(ExceptionCode.EMPTY_LIST);
    }

    return this.flattenDocument(retrieveTraceList.get(0).getSegments());
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

  // this method will hit a http endpoints of sample web apps
  private String getAppTraceId() throws Exception {
    SampleAppResponse sampleAppResponse = this.caller.callSampleApp();

    return sampleAppResponse.getTraceId();
  }
}
