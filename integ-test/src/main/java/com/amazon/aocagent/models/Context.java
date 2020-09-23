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
package com.amazon.aocagent.models;

import com.amazon.aocagent.fileconfigs.ECSTaskDefTemplate;
import com.amazon.aocagent.fileconfigs.ExpectedMetric;
import com.amazon.aocagent.fileconfigs.ExpectedTrace;
import com.amazon.aocagent.fileconfigs.OTConfig;
import com.amazon.aocagent.helpers.TempDirHelper;
import com.amazon.aocagent.testamis.ITestAMI;
import com.amazonaws.services.ec2.model.Subnet;
import lombok.Data;

import java.util.List;

@Data
public class Context {
  private String stackFilePath;
  private Stack stack;
  private String agentVersion;
  private String localPackagesDir;
  private ITestAMI testingAMI;
  private String instanceId;
  private String instancePublicIpAddress;
  private String instancePrivateIpAddress;
  private String githubSha;
  private OTConfig otConfig;
  private ExpectedMetric expectedMetric;
  private ExpectedTrace expectedTrace;
  private String expectedTraceId;
  private List<String> expectedSpanIdList;

  /** AWS account default Security Group Id. */
  private String defaultSecurityGrpId;

  /** AWS account default VPC Id. */
  private String defaultVpcId;

  /** AWS account default subnets. */
  private List<Subnet> defaultSubnets;

  /** ECS Service launch type. Eg, EC2 or Fargate. */
  private String ecsLaunchType;

  /** ECS deployment mode. Eg, SIDECAR or DaemonSet. */
  private String ecsDeploymentMode;

  /** ECS cluster name. */
  private String ecsClusterName;

  /** ECS task role arn. */
  private String ecsTaskRoleArn;

  /** ECS task execution role arn. */
  private String ecsExecutionRoleArn;

  /** ECS task def. */
  private ECSTaskDefTemplate ecsTaskDef;

  /** ECS data emitter image. */
  private String dataEmitterImage;

  /** AOC image for testing. */
  private String aocImage;

  /** Test resources region. */
  private String region;

  /** EKS cluster attributes. */
  private String eksEndpoint;

  /** EKS cluster certificate. */
  private String eksCertificate;

  /** EKS cluster name. */
  private String eksClusterName;

  /** kubectl binary path. */
  private String kubectlPath;

  /** kubeconfig path. */
  private String kubeconfigPath;

  /** EKS iam authenticator binary path. */
  private String iamAuthenticatorPath;

  /** EKS test manifest file name. */
  private String eksTestManifestName;

  /** EKS test artifacts dir. */
  private TempDirHelper eksTestArtifactsDir;
}
