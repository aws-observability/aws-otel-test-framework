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

package com.amazon.aoc.exception;

public enum ExceptionCode {
  LOCAL_PACKAGE_NOT_EXIST(20000, "local package not exist"),
  S3_KEY_ALREADY_EXIST(20001, "s3 key is existed already"),
  SSH_COMMAND_FAILED(20002, "ssh command failed"),
  LOGIN_USER_NOT_FOUND(20003, "login user not found"),
  FAILED_AFTER_RETRY(20004, "failed after retry"),
  EC2INSTANCE_STATUS_PENDING(20005, "ec2 instance status is pending"),
  EC2INSTANCE_STATUS_BAD(20006, "ec2 instance status is bad"),
  NO_MATCHED_S3_KEY(20007, "no matched s3 key for this ami"),
  NO_MATCHED_DOWNLOADING_COMMAND(20008, "no matched downloading command for this ami"),
  NO_MATCHED_PACKAGE_NAME(20009, "no matched package name for this ami"),
  NO_MATCHED_INSTALLING_COMMAND(20010, "no matched installing command for this ami"),
  NO_MATCHED_STARTING_COMMAND(20010, "no matched starting command for this ami"),
  NO_MATCHED_DOCKER_INSTALLING_COMMAND(20010, "no matched docker installing command for this ami"),
  COMMAND_FAILED_TO_EXECUTE(20011, "command failed to execute"),
  STACK_FILE_NOT_FOUND(20012, "stack file not found, please setup it"),
  S3_BUCKET_IS_EXISTED_IN_CURRENT_ACCOUNT(20013, "s3 bucket is already existed in your account"),
  S3_BUCKET_IS_EXISTED_GLOBALLY(20014, "s3 bucket is already existed globally"),
  NO_DEFAULT_SECURITY_GROUP(20015, "no default security group found"),
  NO_AVAILABLE_SUBNET(20016, "no available subnet found for vcp"),
  ECS_INSTANCE_NOT_READY(20030, "ecs container instance is not ready"),
  ECS_CLUSTER_NOT_EXIST(20031, "ecs cluster is not existed"),
  ECS_TASK_EXECUTION_FAIL(20032, "ecs cluster task failed to start"),
  ECS_TASK_DEFINITION_PARSE_FAIL(20033, "fail to parse ecs task definition template"),

  EXPECTED_METRIC_NOT_FOUND(30001, "expected metric not found"),

  VERSION_NOT_MATCHED(40001, "version is not matched in the candidate package"),
  GITHUB_SHA_NOT_MATCHED(40002, "github sha is not matched in the candidate package"),

  // validating errors
  TRACE_ID_NOT_MATCHED(50001, "trace id not matched"),
  TRACE_SPAN_LIST_NOT_MATCHED(50002, "trace span list has different length"),
  TRACE_SPAN_NOT_MATCHED(50003, "trace span not matched"),
  TRACE_LIST_NOT_MATCHED(50004, "trace list has different length"),

  // eks option errors
  EKS_CLUSTER_UNAVAIL(60001, "cluster is not available"),
  EKS_CLUSTER_NAME_UNAVAIL(60002, "cluster name is not available"),
  EKS_KUBECTL_PATH_UNAVAIL(60003, "kubectl path is not available"),
  EKS_IAM_AUTHENTICATOR_PATH_UNAVAIL(60004, "aws-iam-authenticator path is not available"),
  ;
  private int code;
  private String message;

  ExceptionCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
