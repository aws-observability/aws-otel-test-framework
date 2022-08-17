package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.TaskService;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.Task;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ECSHealthCheckValidator implements IValidator {
  // TODO : need to verify default count
  private static final int DEFAULT_MAX_RETRY_COUNT = 5;
  private static final int CHECK_INTERVAL_IN_MILLI = 30 * 1000;
  private Context context;
  private FileConfig expectedMetric;

  private TaskService taskService;


  @Override
  public void init(Context context, ValidationConfig validationConfig,
                   ICaller caller, FileConfig expectedDataTemplate) throws Exception {
    this.context = context;
    this.expectedMetric = expectedDataTemplate;
    this.taskService = new TaskService();
  }

  @Override
  public void validate() throws Exception {
    log.info("allow sample app load balancer to start");
    // TODO : check if following sleep is needed or not
    //TimeUnit.SECONDS.sleep(60);
    log.info("[ECSHealthCheckValidator] start validating ECS Health Check");

    RetryHelper.retry(DEFAULT_MAX_RETRY_COUNT, CHECK_INTERVAL_IN_MILLI, true, () -> {
      if (context.getEcsContext() != null && context.getEcsContext().getEcsTaskArn() != null) {
        DescribeTasksResult result =
                taskService.describeTask(context.getEcsContext().getEcsTaskArn());
        if (result != null && result.getTasks() != null && !result.getTasks().isEmpty()) {
          Task task = result.getTasks().get(0);
          if (task != null && !task.getContainers().isEmpty()) {
            String status = task.getContainers().get(1).getHealthStatus();
            if (status.equalsIgnoreCase("healthy")) {
              return;
            }
          }
          throw new BaseException(
                  ExceptionCode.ECS_HEALTH_VALIDATION_FAILED,
                  "[ECSHealthCheckValidator] Validation failed for Health check");
        } else {
          throw new BaseException(
                  ExceptionCode.ECS_DESCRIBE_TASK_FAILED,
                  "[ECSHealthCheckValidator] Validation failed for Health check");
        }
      } else {
        log.info("[ECSHealthCheckValidator] ECS task Arn is null!");
      }
    });


    log.info("[ECSHealthCheckValidator] end validating ECS Health Check");
  }
}
