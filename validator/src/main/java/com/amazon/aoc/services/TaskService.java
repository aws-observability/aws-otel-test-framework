package com.amazon.aoc.services;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import lombok.extern.log4j.Log4j2;


/**
 * a wrapper of ecs service client.
 */
@Log4j2
public class TaskService {
  private AmazonECS client;

  public TaskService() {
    client = AmazonECSClientBuilder.standard().build();
  }

  /**
   * Method to call Amazon AWS Describe task API.
   * @param clusterArn Cluster Arn
   * @return
   */
  public DescribeTasksResult describeTask(String clusterArn) {
    String taskArn = this.listTasks(clusterArn);
    log.info("[TaskService] Task Arn : " + taskArn);
    DescribeTasksRequest re = new DescribeTasksRequest().withTasks(taskArn).withCluster(clusterArn);
    log.info("[TaskService] DescribeTasksRequest : " + re);
    DescribeTasksResult result = client.describeTasks(re);
    log.info("[TaskService] DescribeTasksResult : " + result);
    return result;
  }

  /**
   * Method to call Amazon AWS listTask API.
   * @param clusterArn Cluster Arn
   * @return
   */
  public String listTasks(String clusterArn) {
    ListTasksRequest listTasksRequest = new ListTasksRequest().withCluster(clusterArn);
    ListTasksResult result =  client.listTasks(listTasksRequest);
    if (result != null && !result.getTaskArns().isEmpty()) {
      return result.getTaskArns().get(0);
    }
    return null;
  }
}
