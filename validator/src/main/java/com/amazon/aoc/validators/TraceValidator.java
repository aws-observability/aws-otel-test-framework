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

import com.amazon.aoc.enums.GenericConstants;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.SampleAppResponse;
import com.github.wnameless.json.flattener.JsonFlattener;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TraceValidator extends XrayValidator {

  @Override
  public void validate() throws Exception {
    // 2 retries for calling the sample app to handle the Lambda case,
    // where first request might be a cold start and have an additional unexpected subsegment
    boolean isMatched =
        RetryHelper.retry(
            2,
            Integer.parseInt(GenericConstants.SLEEP_IN_MILLISECONDS.getVal()),
            false,
            () -> {
              // Call sample app and get locally stored trace
              Map<String, Object> storedTrace = this.getStoredTrace();
              log.info("value of stored trace map: {}", storedTrace);

              // prepare trace ID to retrieve from X-Ray service
              String traceId = (String) storedTrace.get("[0].trace_id");

              // Retry 5 times to since segments might not be immediately available in X-Ray service
              RetryHelper.retry(
                  5,
                  () -> {
                    // get retrieved trace from x-ray service
                    Map<String, Object> actualTrace = this.getActualTrace(traceId);
                    log.info("value of actual trace map: {}", actualTrace);

                    // data model validation of other fields of segment document
                    for (Map.Entry<String, Object> entry : storedTrace.entrySet()) {
                      String targetKey = entry.getKey();
                      if (actualTrace.get(targetKey) == null) {
                        log.error("mis target data: {}", targetKey);
                        throw new BaseException(ExceptionCode.DATA_MODEL_NOT_MATCHED);
                      }

                      Pattern p =
                          Pattern.compile(entry.getValue().toString(), Pattern.CASE_INSENSITIVE);
                      Matcher m = p.matcher(actualTrace.get(targetKey).toString());
                      if (!m.matches()) {
                        log.error("data model validation failed");
                        log.info("mis matched data model field list");
                        log.info("value of stored trace map: {}", entry.getValue());
                        log.info("value of actual trace map: {}", actualTrace.get(entry.getKey()));
                        log.info("==========================================");
                        throw new BaseException(ExceptionCode.DATA_MODEL_NOT_MATCHED);
                      }
                    }
                  });
            });

    if (!isMatched) {
      throw new BaseException(ExceptionCode.DATA_MODEL_NOT_MATCHED);
    }

    log.info("validation is passed for path {}", caller.getCallingPath());
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
