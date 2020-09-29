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

package com.amazon.aocagent.enums;

import lombok.Getter;

@Getter
public enum GenericConstants {

  // stack related
  DEFAULT_STACK_FILE_PATH(".aoc-stack.yml"),
  DEFAULT_REGION("us-west-2"),
  DEFAULT_S3_RELEASE_CANDIDATE_BUCKET("aoc-release-candidate"),
  DEFAULT_S3_BUCKET("aws-observability-collector-test"),
  DEFAULT_SSH_KEY_S3_BUCKET_NAME("aoc-ssh-key"),
  DEFAULT_TRACE_S3_BUCKET_NAME("trace-expected-data"),
  DEFAULT_DOCKER_IMAGE_REPO_NAME("josephwy/aws-observability-collector"),

  // release related
  PACKAGE_NAME_PREFIX("aws-observability-collector."),
  LOCAL_PACKAGES_DIR("build/packages"),
  GITHUB_SHA_FILE_NAME("GITHUB_SHA"),

  // ec2 related
  EC2_INSTANCE_TAG_KEY("aoc-integ-test-tag"),
  EC2_INSTANCE_TAG_VAL("aoc-integ-test"),
  DEFAULT_SECURITY_GROUP_NAME("default"),
  SECURITY_GROUP_NAME("aoc-integ-test-sp"),
  IAM_ROLE_NAME("aoc-integ-test-iam-role"),

  // ssm related
  UPDATE_SSM_AGENT_DOCUMENT("AWS-UpdateSSMAgent"),
  RUN_POWER_SHELL_SCRIPT_DOCUMENT("AWS-RunPowerShellScript"),

  // ssh related
  SSH_KEY_NAME("aoc-ssh-key-2020-07-22"),
  SSH_CERT_LOCAL_PATH("sshkey.pem"),
  SSH_TIMEOUT("30000"), // ms

  // retry
  SLEEP_IN_MILLISECONDS("10000"), // ms
  SLEEP_IN_SECONDS("30"),
  MAX_RETRIES("10"),

  // task
  TASK_RESPONSE_FILE_LOCATION("./task_response"),

  // configuration
  EC2_CONFIG_PATH("/tmp/test.yml"),
  EC2_WIN_CONFIG_PATH("C:\\test.yml"),

  // emitter
  TRACE_EMITTER_ENDPOINT("http://localhost:4567/span0"),
  SERVICE_NAMESPACE("AWSObservability"),
  SERVICE_NAME("CloudWatchOTService"),
  TRACE_EMITTER_DOCKER_IMAGE_URL("josephwy/integ-test-emitter:0.9.1"),

  // validator related
  METRIC_NAMESPACE("default"),

  // release candidate related
  CANDIDATE_PACK_TO("build/candidate.tar.gz"),
  CANDIDATE_DOWNLOAD_TO("build/candidate-downloaded.tar.gz"),
  CANDIDATE_UNPACK_TO("."),

  // ECS
  ECS_LAUNCH_TYPE("ecsLaunchType"),
  ECS_DEPLOY_MODE("ecsDeployMode"),
  ECS_TASK_DEF("ecsTaskDef"),
  EC2_INSTANCE_ECS_TAG_VAL("aoc-integ-test-ecs"),
  ECS_SIDECAR_CLUSTER("aoc-sidecar-integ-test"),

  // EKS
  AUTHENTICATOR_PATH("awsAuthenticatorPath"),
  EKS_CLUSTER_NAME("eksClusterName"),
  KUBECTL_PATH("kubectlPath"),
  KUBECONFIG_PATH("kubeconfigPath"),
  TEST_MANIFEST_NAME("eksTestManifestName"),
  EKS_DEFAULT_TEST_MANIFEST("aoc-eks-sidecar"),
  EKS_INTEG_TEST("EKSIntegTest"),

  //Windows
  WINDOWS_CTL_PATH("'C:\\Program Files\\Amazon\\AwsObservabilityCollector"
          + "\\aws-observability-collector-ctl.ps1'"),

  // common constants
  EC2("EC2"),
  FARGATE("FARGATE"),
  DEFAULT("default"),
  AOC_PREFIX("aoc-"),
  AOC_PORT("55680"),
  RESOURCE_CLEAN_THRESHOLD("120"),
  ;

  private String val;

  GenericConstants(String val) {
    this.val = val;
  }
}
