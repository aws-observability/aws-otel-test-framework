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
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.SampleAppResponse;
import com.amazon.aoc.services.XRayService;
import com.amazonaws.services.xray.model.Trace;
import com.github.wnameless.json.flattener.JsonFlattener;
import lombok.extern.log4j.Log4j2;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j2
public class TraceValidator implements IValidator {
  private static int MAX_RETRY_COUNT = 3;
  private XRayService xrayService;
  private ICaller caller;
  private FileConfig expectedTrace;

    @Override
    public void init(Context context, ICaller caller, FileConfig expectedTrace) throws Exception {
        this.xrayService = new XRayService(context.getRegion());
        this.caller = caller;
        this.expectedTrace = expectedTrace;
    }

    @Override
    public void validate() throws Exception {
        // get stored trace
        Map<String, Object> storedTrace = this.getStoredTrace();

        // create trace id list to retrieve trace from x-ray service
        String traceId = (String) storedTrace.get("trace_id");
        List<String> traceIdList = Collections.singletonList(traceId);

        // get retrieved trace from x-ray service
        Map<String, Object> retrievedTrace = this.getRetrievedTrace(traceIdList);

        // validation of trace id
        if (!storedTrace.get("trace_id").equals(retrievedTrace.get("trace_id"))) {
            throw new BaseException(ExceptionCode.TRACE_ID_NOT_MATCHED);
        }

        // data model validation of other fields of segment document
        for (Map.Entry<String, Object> entry : storedTrace.entrySet()) {
            if (!entry.getValue().equals(retrievedTrace.get(entry.getKey()))) {
                log.info("mis matched data model field list");
                log.info("value of stored trace map: {}", entry.getValue());
                log.info("value of retrieved map: {}",retrievedTrace.get(entry.getKey()));
                log.info("==========================================");
                throw new BaseException(ExceptionCode.DATA_MODEL_NOT_MATCHED);
            }
        }
    }

    // this method will hit get trace from x-ray service and get retrieved trace
    private Map<String, Object> getRetrievedTrace(List<String> traceIdList) throws Exception {
        Map<String, Object> flattenedJsonMapForRetrievedTrace = null;

        // retrieve trace from x-ray service
        TimeUnit.SECONDS.sleep(15);
        List<Trace> retrieveTraceList = xrayService.listTraceByIds(traceIdList);

        try {
            // flattened JSON object to a map
            flattenedJsonMapForRetrievedTrace = JsonFlattener.flattenAsMap(retrieveTraceList.get(0).getSegments().get(0).getDocument());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return flattenedJsonMapForRetrievedTrace;
    }

    // this method will hit a http endpoints of sample web apps and get stored trace
    private Map<String, Object> getStoredTrace() throws Exception {
        String PATH = "src/main/resources";
        Map<String, Object> flattenedJsonMapForStoredTraces = null;

        SampleAppResponse sampleAppResponse = this.caller.callSampleApp();

        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(PATH + this.expectedTrace.getPath()));
            JSONObject jsonObject = (JSONObject) obj;

            // flattened JSON object to a map
            flattenedJsonMapForStoredTraces = JsonFlattener.flattenAsMap(jsonObject.toString());
            flattenedJsonMapForStoredTraces.put("trace_id", sampleAppResponse.getTraceId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return flattenedJsonMapForStoredTraces;
    }
}