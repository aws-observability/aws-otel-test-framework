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

import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.extern.log4j.Log4j2;

import java.io.File;

/** S3Service is the wrapper of Amazon S3 Client. */
@Log4j2
public class S3Service {
  private AmazonS3 amazonS3;

  public S3Service(String region) {
    amazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).build();
  }

  /**
   * uploadS3Object uploads the localfile to the s3 bucket.
   *
   * @param localFilePath the path of local file to be
   * @param bucketName the s3 bucket name
   * @param key the s3 key name
   * @param override override the s3 key if it's already existed when override is true
   * @throws BaseException when the s3 key is already existed and override is false
   */
  public void uploadS3Object(String localFilePath, String bucketName, String key, boolean override)
      throws BaseException {
    log.info("upload {}/{} to s3", bucketName, key);
    this.uploadS3Object(
        localFilePath, bucketName, key, override, false, null, CannedAccessControlList.PublicRead);
  }

  private void uploadS3Object(
      String localFilePath,
      String bucketName,
      String key,
      boolean override,
      boolean exceptionOnKeyExisting,
      ObjectMetadata objectMetadata,
      CannedAccessControlList accessControlList)
      throws BaseException {
    // create Bucket if not existed
    if (!amazonS3.doesBucketExistV2(bucketName)) {
      amazonS3.createBucket(bucketName);
    }

    // check if the key is existed
    if (!override && amazonS3.doesObjectExist(bucketName, key)) {
      if (exceptionOnKeyExisting) {
        throw new BaseException(
            ExceptionCode.S3_KEY_ALREADY_EXIST, "s3 key is already existed: " + key);
      } else {
        log.warn("s3 key is already existed: {}, skip", key);
        return;
      }
    }

    PutObjectRequest putObjectRequest =
        new PutObjectRequest(bucketName, key, new File(localFilePath))
            .withCannedAcl(accessControlList);
    if (objectMetadata != null) {
      putObjectRequest.setMetadata(objectMetadata);
    }

    amazonS3.putObject(putObjectRequest);
  }

  /**
   * uploadS3ObjectWithPrivateAccess uploads the locafile to the s3 bucket with private access.
   *
   * @param localFilePath the path of local file to be
   * @param bucketName the s3 bucket name
   * @param key the s3 key name
   * @param override override the s3 key if it's already existed when override is true
   * @throws BaseException when the s3 key is already existed and override is false
   */
  public void uploadS3ObjectWithPrivateAccess(
      String localFilePath, String bucketName, String key, boolean override) throws BaseException {
    this.uploadS3Object(
        localFilePath, bucketName, key, override, false, null, CannedAccessControlList.Private);
  }

  /**
   * downloadS3Object downloads the s3 object to local.
   *
   * @param bucketName the s3 bucket name
   * @param key the s3 object key name
   * @param toLocation the local location to download to
   */
  public void downloadS3Object(String bucketName, String key, String toLocation) {
    log.info("download s3 object {}/{}", bucketName, key);
    amazonS3.getObject(new GetObjectRequest(bucketName, key), new File(toLocation));
  }

  /**
   * getS3ObjectAsString get s3 object and transfer it to string.
   *
   * @param bucketName the s3 bucket name
   * @param key the s3 object key name
   * @return object content as string
   */
  public String getS3ObjectAsString(String bucketName, String key) {
    log.info("get s3 object as string {}/{}", bucketName, key);
    return amazonS3.getObjectAsString(bucketName, key);
  }

  /**
   * create s3 bucket base on the bucket name.
   *
   * @param bucketName s3 bucket name
   * @throws BaseException when the bucket is already existed globally, or the bucket is already
   *     existed in the current aws account.
   */
  public void createBucket(String bucketName) throws BaseException {
    // check if the bucket exist globally
    if (!amazonS3.doesBucketExistV2(bucketName)) {
      amazonS3.createBucket(bucketName);
      return;
    }

    // check if the bucket is already existed in the current aws account
    HeadBucketRequest headBucketRequest = new HeadBucketRequest(bucketName);
    try {
      amazonS3.headBucket(headBucketRequest);
    } catch (Exception ex) {
      log.info(ex.getMessage());
      // this bucket is not existed in this account
      throw new BaseException(
          ExceptionCode.S3_BUCKET_IS_EXISTED_GLOBALLY,
          "this bucket " + bucketName + " is existed globally");
    }

    throw new BaseException(ExceptionCode.S3_BUCKET_IS_EXISTED_IN_CURRENT_ACCOUNT);
  }
}
