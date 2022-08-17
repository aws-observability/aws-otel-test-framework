package com.amazon.aoc.services;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
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

  public DescribeTasksResult describeTask(String taskArn) {
    DescribeTasksRequest request = new DescribeTasksRequest().withTasks(taskArn);
    return client.describeTasks(request);
  }
}
