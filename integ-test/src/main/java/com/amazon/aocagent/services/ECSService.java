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
package com.amazon.aocagent.services;

import com.amazon.aocagent.enums.GenericConstants;
import com.amazon.aocagent.exception.BaseException;
import com.amazon.aocagent.exception.ExceptionCode;
import com.amazon.aocagent.helpers.RetryHelper;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateClusterResult;
import com.amazonaws.services.ecs.model.DeleteClusterRequest;
import com.amazonaws.services.ecs.model.DeregisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.ecs.model.ListTaskDefinitionsResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionResult;
import com.amazonaws.services.ecs.model.RunTaskRequest;
import com.amazonaws.services.ecs.model.RunTaskResult;
import com.amazonaws.services.ecs.model.StopTaskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
public class ECSService {
  private AmazonECS ecsClient;
  private ObjectMapper jsonMapper;

  public ECSService(final String region) {
    this.ecsClient = AmazonECSClientBuilder.standard().withRegion(region).build();
    this.jsonMapper = new ObjectMapper();
  }

  /**
   * create ECS cluster for integ testing.
   * @return cluster creation result
   */
  public CreateClusterResult createCluster(final String clusterName) {
    CreateClusterRequest request =
        new CreateClusterRequest().withClusterName(clusterName);
    log.info("creating ECS cluster: {}", clusterName);
    return ecsClient.createCluster(request);
  }

  /**
   * create and register ecs task definition for both EC2 and fargate running mode.
   * @param taskDef ecs task definition
   * @return {@link RegisterTaskDefinitionResult} for registering ecs container defs
   */
  public RegisterTaskDefinitionResult registerTaskDefinition(String taskDef) throws BaseException {
    RegisterTaskDefinitionRequest request;

    try {
      request = jsonMapper.readValue(taskDef, RegisterTaskDefinitionRequest.class);
    } catch (Exception e) {
      throw new BaseException(ExceptionCode.ECS_TASK_DEFINITION_PARSE_FAIL, e.getMessage());
    }
    return ecsClient.registerTaskDefinition(request);
  }

  /**
   * create and run ECS task definitions based on the launch type.
   * @param runTaskRequest ecs run task request
   */
  public void runTaskDefinition(RunTaskRequest runTaskRequest) throws BaseException {

    final RunTaskResult runTaskResult = ecsClient.runTask(runTaskRequest);
    runTaskResult
        .getTasks()
        .forEach(task -> log.info("Successfully running task [{}]", task.getTaskArn()));

    if (!runTaskResult.getFailures().isEmpty()) {
      throw new BaseException(
          ExceptionCode.ECS_TASK_EXECUTION_FAIL, runTaskResult.getFailures().toString());
    }
  }

  /**
   * clean ECS tasks resources.
   * @throws InterruptedException fail to clean exception
   */
  public void cleanTasks() throws Exception {
    List<String> clusters = this.listClusters();
    for (String clusterName : clusters) {
      // clean up tasks
      for (String taskArn :
          ecsClient
              .listTasks(
                  new ListTasksRequest()
                      .withCluster(clusterName)
                      .withDesiredStatus(DesiredStatus.RUNNING))
              .getTaskArns()) {
        ecsClient.stopTask(new StopTaskRequest().withTask(taskArn).withCluster(clusterName));
      }

      // make sure there is no RUNNING status task remaining
      RetryHelper.retry(
          () -> {
            if (ecsClient
                .listTasks(
                    new ListTasksRequest()
                        .withCluster(clusterName)
                        .withDesiredStatus(DesiredStatus.RUNNING))
                .getTaskArns()
                .isEmpty()) {
              return;
            }
          });

      log.info("finish metric validation");
    }
  }

  /**
   * clean ECS cluster.
   */
  public void cleanCluster() {
    for (String name : listClusters()) {
      if (!name.startsWith(GenericConstants.ECS_SIDECAR_CLUSTER.getVal())
          || !isClusterTwoHrsOlder(name)) {
        continue;
      }
      DeleteClusterRequest request = new DeleteClusterRequest().withCluster(name);
      try {
        ecsClient.deleteCluster(request);
      } catch (Exception e) {
        log.error("{} can't be deleted", name, e);
      }
    }
  }

  private boolean isClusterTwoHrsOlder(String name) {
    String createdTimestamp = name.substring(name.lastIndexOf("-") + 1);
    if ((System.currentTimeMillis() - Long.valueOf(createdTimestamp))
        > TimeUnit.HOURS.toMillis(2)) {
      return true;
    }
    return false;
  }

  private List<String> listClusters() {
    ListClustersResult clusters = ecsClient.listClusters();
    return clusters.getClusterArns().stream()
        .map(arn -> arn.substring(arn.lastIndexOf("/") + 1))
        .collect(Collectors.toList());
  }

  /**
   * clean ECS tasks with prefix.
   * @param prefix task prefix
   */
  public void cleanTaskDefinitions(String prefix) {
    ListTaskDefinitionsResult result = ecsClient.listTaskDefinitions();
    result.getTaskDefinitionArns().stream()
        .filter(arn -> arn.indexOf(prefix) > 0)
        .forEach(
            arn ->
                ecsClient.deregisterTaskDefinition(
                    new DeregisterTaskDefinitionRequest().withTaskDefinition(arn)));
    log.info("result {}", result.getTaskDefinitionArns());
  }

  /**
   * check if ECS Cluster has available container instance.
   * @param clusterName cluster name
   * @throws Exception fail to wait for container instance ready
   */
  public void waitForContainerInstanceRegistered(String clusterName) throws Exception {
    RetryHelper.retry(
        () -> {
          Optional<Cluster> clusterOpt = describeCluster(clusterName);
          if (!clusterOpt.isPresent()) {
            log.warn("{} is not created", clusterName);
            throw new BaseException(ExceptionCode.ECS_CLUSTER_NOT_EXIST);
          }
          Cluster ecsCluster = clusterOpt.get();
          if (ecsCluster.getRegisteredContainerInstancesCount() == 0) {
            log.warn(
                "waiting for ecs container instance to be ready - {}", clusterName);
            throw new BaseException(ExceptionCode.ECS_INSTANCE_NOT_READY);
          }
        });
  }

  /**
   * describe ECS cluster.
   * @param clusterName cluster name
   * @return
   */
  public Optional<Cluster> describeCluster(String clusterName) {
    DescribeClustersRequest request = new DescribeClustersRequest().withClusters(clusterName);
    DescribeClustersResult response = ecsClient.describeClusters(request);
    List<Cluster> clusters = response.getClusters();
    if (clusters.isEmpty()) {
      return Optional.ofNullable(null);
    }
    return Optional.of(clusters.get(0));
  }

  /**
   * check if container instances are ready for using.
   * @param clusterName cluster name
   * @return
   */
  public boolean isContainerInstanceAvail(String clusterName) {
    Optional<Cluster> clusterOpt = describeCluster(clusterName);
    if (!clusterOpt.isPresent()) {
      return false;
    }
    Cluster cluster = clusterOpt.get();
    return cluster.getRegisteredContainerInstancesCount() > 0;
  }
}
