package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.TaskService;
import com.amazonaws.services.ecs.model.Container;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.Task;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ECSHealthCheckValidator implements IValidator {
    private static final int CHECK_INTERVAL_IN_MILLI = 30 * 1000;
    private Context context;
    private int retryCount;
    private FileConfig expectedMetric;

    private TaskService taskService;

    public ECSHealthCheckValidator(TaskService taskService, int retryCount) {
        this.taskService = taskService;
        this.retryCount = retryCount;
    }

    @Override
    public void init(Context context, ValidationConfig validationConfig, ICaller caller,
            FileConfig expectedDataTemplate) throws Exception {
        this.context = context;
        this.expectedMetric = expectedDataTemplate;
    }

    @Override
    public void validate() throws Exception {
        log.info("[ECSHealthCheckValidator] start validating ECS Health Check");
        RetryHelper.retry(retryCount, CHECK_INTERVAL_IN_MILLI, true, () -> {
            if (context == null || context.getEcsContext() == null
                    || context.getEcsContext().getEcsClusterArn() == null) {
                throw new BaseException(ExceptionCode.ECS_RESOURCES_NOT_FOUND,
                        "[ECSHealthCheckValidator] ECSContext is not set");
            }

            // get all the taskARNs running inside the cluster (set in ECSContext)
            List<String> taskArns = taskService.listTasks(context.getEcsContext().getEcsClusterArn());

            if (taskArns == null || taskArns.isEmpty()) {
                throw new BaseException(ExceptionCode.ECS_RESOURCES_NOT_FOUND,
                        "[ECSHealthCheckValidator] ListTask service returned null tasks");
            }
            // get details of all tasks running inside the cluster (as set in ECSContext)
            DescribeTasksResult result = taskService.describeTask(taskArns, context.getEcsContext().getEcsClusterArn());

            if (result == null || result.getTasks() == null || result.getTasks().isEmpty()) {
                throw new BaseException(ExceptionCode.ECS_RESOURCES_NOT_FOUND,
                        "[ECSHealthCheckValidator] DescribeTask service returned null result");
            }
            // filter tasks with task definition (as set in ECSContext) and has 'running'
            // status
            List<Task> aocTasks = result.getTasks().stream()
                    .filter(t -> t.getTaskDefinitionArn().equalsIgnoreCase(context.getEcsContext().getEcsTaskDefArn())
                            && t.getLastStatus().equalsIgnoreCase("RUNNING"))
                    .collect(Collectors.toList());

            if (aocTasks == null || aocTasks.isEmpty()) {
                throw new BaseException(ExceptionCode.ECS_RESOURCES_NOT_FOUND,
                        "[ECSHealthCheckValidator] running tasks with specified taskdef " + "were not found");
            }

            // Validating the status of one of the aocTasks found with the specified task
            // definition
            Task task = aocTasks.get(0);
            // get the 'aoc-collector' containers running inside the task
            List<Container> containers = task.getContainers().stream()
                    .filter(container -> container.getName().equalsIgnoreCase("aoc-collector"))
                    .collect(Collectors.toList());

            if (containers == null || containers.isEmpty()) {
                throw new BaseException(ExceptionCode.ECS_RESOURCES_NOT_FOUND,
                        "[ECSHealthCheckValidator] no 'aoc-collector' containers were "
                                + "found inside the specified task");
            }

            // check the health_status of 'aoc-containers'
            for (Container container : containers) {
                if (!container.getHealthStatus().equalsIgnoreCase("HEALTHY")) {
                    throw new BaseException(ExceptionCode.HEALTH_STATUS_NOT_MATCHED,
                            "[ECSHealthCheckValidator] health_status was not found healthy");
                }
            }
        });
        log.info("[ECSHealthCheckValidator] end validating ECS Health Check");
    }
}
