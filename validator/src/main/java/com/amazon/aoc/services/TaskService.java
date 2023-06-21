package com.amazon.aoc.services;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import java.util.List;
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
	 * 
	 * @param clusterArn
	 *            Cluster Arn
	 * @return
	 */
	public DescribeTasksResult describeTask(List<String> taskArns, String clusterArn) {
		DescribeTasksRequest re = new DescribeTasksRequest().withTasks(taskArns).withCluster(clusterArn);
		DescribeTasksResult result = client.describeTasks(re);
		return result;
	}

	/**
	 * Method to call Amazon AWS listTask API.
	 * 
	 * @param clusterArn
	 *            Cluster Arn
	 * @return
	 */
	public List<String> listTasks(String clusterArn) {
		ListTasksRequest listTasksRequest = new ListTasksRequest().withCluster(clusterArn);
		ListTasksResult result = client.listTasks(listTasksRequest);
		if (result != null && !result.getTaskArns().isEmpty()) {
			return result.getTaskArns();
		}
		return null;
	}
}
