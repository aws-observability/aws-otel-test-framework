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

package com.amazon.aoc.services;

import com.amazonaws.services.xray.AWSXRay;
import com.amazonaws.services.xray.AWSXRayClientBuilder;
import com.amazonaws.services.xray.model.BatchGetTracesRequest;
import com.amazonaws.services.xray.model.BatchGetTracesResult;
import com.amazonaws.services.xray.model.Trace;
import java.util.List;

public class XRayService {
  private AWSXRay awsxRay;

  public XRayService(String region) {
    awsxRay = AWSXRayClientBuilder.standard().withRegion(region).build();
  }

  /**
   * List trace objects by ids.
   *
   * @param traceIdList trace id list
   * @return trace object list
   */
  public List<Trace> listTraceByIds(List<String> traceIdList) {
    BatchGetTracesResult batchGetTracesResult =
        awsxRay.batchGetTraces(new BatchGetTracesRequest().withTraceIds(traceIdList));

    return batchGetTracesResult.getTraces();
  }
}
