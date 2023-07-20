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
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.SampleAppResponse;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.XRayService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LoadBalancingValidator extends XrayValidator {

  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedTrace,
      XRayService xrayService)
      throws Exception {
    init(context, validationConfig, caller, expectedTrace);
    this.xrayService = xrayService;
  }

  @Override
  public void validate() throws Exception {
    // Run the validation until 5 successful matches or until an exception is thrown
    int successes = 0;
    while (successes < 5) {
      // Call sample app and get traceId
      String traceId = this.getSampleAppResponse();
      List<String> traceIdList = Collections.singletonList(traceId);

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

            List<String> collectorIdList =
                retrievedTrace.keySet().stream()
                    .filter(s -> s.endsWith("collector-id"))
                    .collect(Collectors.toList());
            if (!checkSpanCount(collectorIdList)) {
              throw new BaseException(ExceptionCode.NOT_ENOUGH_SPANS);
            }

            String targetCollectorId = retrievedTrace.get(collectorIdList.get(0)).toString();
            for (String collectorIdKey : collectorIdList.subList(1, collectorIdList.size())) {
              if (!targetCollectorId.equals(retrievedTrace.get(collectorIdKey).toString())) {
                log.info("id values do not match");
                log.info("value of target id: {}", targetCollectorId);
                log.info(
                    "value of retrieved id: {}", retrievedTrace.get(collectorIdKey).toString());
                log.info("==========================================");
                throw new BaseException(ExceptionCode.COLLECTOR_ID_NOT_MATCHED);
              }
            }
          });

      successes += 1;
    }

    log.info("validation is passed for path {}", caller.getCallingPath());
  }

  // this method will hit a http endpoints of sample web apps and return the traceId
  private String getSampleAppResponse() throws Exception {
    SampleAppResponse sampleAppResponse = this.caller.callSampleApp();

    return sampleAppResponse.getTraceId();
  }

  private boolean checkSpanCount(List<String> spanSet) throws Exception {
    if (spanSet.size() < 2) {
      log.error("not enough spans in trace map (need at least 2)");
      log.info("==========================================");
      return false;
    }
    return true;
  }
}
