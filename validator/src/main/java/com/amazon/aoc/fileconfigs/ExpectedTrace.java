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

package com.amazon.aoc.fileconfigs;

import lombok.Getter;

@Getter
public enum ExpectedTrace implements FileConfig {
  DEFAULT_EXPECTED_TRACE("/expected-data-template/defaultExpectedTrace.mustache"),
  OTEL_SDK_AWSSDK_EXPECTED_TRACE("/expected-data-template/otelSDKexpectedAWSSDKTrace.mustache"),
  OTEL_SDK_HTTP_EXPECTED_TRACE("/expected-data-template/otelSDKexpectedHTTPTrace.mustache"),
  XRAY_SDK_AWSSDK_EXPECTED_TRACE("/expected-data-template/xraySDKexpectedAWSSDKTrace.mustache"),
  XRAY_SDK_HTTP_EXPECTED_TRACE("/expected-data-template/xraySDKexpectedHTTPTrace.mustache"),
  XRAY_RECEIVER_SDK_HTTP_EXPECTED_TRACE("/expected-data-template/xrayReceiverSDKexpectedHttpSDKTrace.mustache"),
  XRAY_RECEIVER_SDK_AWS_EXPECTED_TRACE("/expected-data-template/xrayReceiverSDKexpectedAWSSDKTrace.mustache"),
  ;

  private String path;

  ExpectedTrace(String path) {
    this.path = path;
  }
}
