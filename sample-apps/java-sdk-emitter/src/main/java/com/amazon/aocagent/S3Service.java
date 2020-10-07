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
package com.amazon.aocagent;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;

public class S3Service {
  private AmazonS3 amazonS3;
  private static final String ENV_TRACE_BUCKET = "TRACE_DATA_BUCKET";
  private static final String ENV_TRACE_S3_KEY = "TRACE_DATA_S3_KEY";

  public S3Service(String region){
    amazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).build();
  }

  public void uploadTraceData(Response traceData) throws JsonProcessingException {
    String bucketName = System.getenv(ENV_TRACE_BUCKET);
    String keyName = System.getenv(ENV_TRACE_S3_KEY);

    if(bucketName == null || bucketName.trim().equals("")){
      throw new RuntimeException("bucketName is empty");
    }

    if(keyName == null || keyName.trim().equals("")){
      throw new RuntimeException("keyName is empty");
    }

    this.uploadS3Object(traceData.toJson(), bucketName, keyName);

  }

  private void uploadS3Object(String data, String bucketName, String key){
    // create Bucket if not existed
    if (!amazonS3.doesBucketExistV2(bucketName)) {
      amazonS3.createBucket(bucketName);
    }

    amazonS3.putObject(bucketName, key, data);
  }
}
