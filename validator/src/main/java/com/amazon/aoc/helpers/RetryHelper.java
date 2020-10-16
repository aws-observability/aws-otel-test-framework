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

package com.amazon.aoc.helpers;

import com.amazon.aoc.enums.GenericConstants;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

@Log4j2
public class RetryHelper {
  /**
   * retry executes the lambda, retry if the lambda throw exceptions.
   *
   * @param retryCount the total retry count
   * @param sleepInMilliSeconds sleep time among retries
   * @param retryable the lambda
   * @throws Exception when the retry count is reached
   */
  public static void retry(int retryCount, int sleepInMilliSeconds, Retryable retryable)
      throws Exception {
    while (retryCount-- > 0) {
      try {
        log.info("retry attempt left : {} ", retryCount);
        retryable.execute();
        return;
      } catch (Exception ex) {
        log.error("exception during retry, you may ignore it", ex);
        TimeUnit.MILLISECONDS.sleep(sleepInMilliSeconds);
      }
    }

    throw new BaseException(ExceptionCode.FAILED_AFTER_RETRY);
  }

  /**
   * retry executes lambda with default retry count(10) and sleep seconds(10).
   *
   * @param retryable the lambda
   * @throws Exception when the retry count is reached
   */
  public static void retry(Retryable retryable) throws Exception {
    retry(
        Integer.valueOf(GenericConstants.MAX_RETRIES.getVal()),
        Integer.valueOf(GenericConstants.SLEEP_IN_MILLISECONDS.getVal()),
        retryable);
  }

  /**
   * retry executes lambda with default sleeping seconds 10s.
   *
   * @param retryCount the total retry count
   * @param retryable the lambda function to be executed
   * @throws Exception when the retry count is reached
   */
  public static void retry(int retryCount, Retryable retryable) throws Exception {
    retry(retryCount, Integer.valueOf(GenericConstants.SLEEP_IN_MILLISECONDS.getVal()), retryable);
  }
}
