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
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ECSHealthCheckValidator implements IValidator {
  // TODO : need to verify default count
  private static final int DEFAULT_MAX_RETRY_COUNT = 5;
  private static final int CHECK_INTERVAL_IN_MILLI = 15 * 1000;
  private Context context;
  private int retryCount;
  private FileConfig expectedMetric;

  private TaskService taskService;

  public ECSHealthCheckValidator(TaskService taskService, int retryCount) {
    this.taskService = taskService;
    this.retryCount = retryCount;
  }

  @Override
  public void init(Context context, ValidationConfig validationConfig,
                   ICaller caller, FileConfig expectedDataTemplate) throws Exception {
    this.context = context;
    this.expectedMetric = expectedDataTemplate;
  }

  @Override
  public void validate() throws Exception {
    log.info("allow sample app load balancer to start");
    TimeUnit.SECONDS.sleep(15);
    log.info("[ECSHealthCheckValidator] start validating ECS Health Check");
    AtomicBoolean validationSuccessFlag = new AtomicBoolean(false);
    RetryHelper.retry((retryCount == 0 ? DEFAULT_MAX_RETRY_COUNT : retryCount),
        CHECK_INTERVAL_IN_MILLI, true, () -> {
        if (context != null && context.getEcsContext() != null
                && context.getEcsContext().getEcsClusterArn() != null) {
          DescribeTasksResult result =
                  taskService.describeTask(context.getEcsContext().getEcsClusterArn());
          if (result != null && result.getTasks() != null && !result.getTasks().isEmpty()) {
            Task task = result.getTasks().get(0);
            if (task != null && !task.getContainers().isEmpty()) {
              List<Container> containers = task.getContainers().stream()
                      .filter(container -> container.getName().equalsIgnoreCase("aoc-collector"))
                      .collect(Collectors.toList());
              for (Container container : containers) {
                if (container.getHealthStatus().equalsIgnoreCase("HEALTHY")) {
                  log.info("[ECSHealthCheckValidator] CheckStatus: " + container.getHealthStatus());
                  validationSuccessFlag.set(true);
                  break;
                }
              }
            }
          }
        }
        if (!validationSuccessFlag.get()) {
          throw new BaseException(
                  ExceptionCode.ECS_RESOURCES_NOT_READY,
                  "[ECSHealthCheckValidator] Awaiting on ECS Resources to be ready");
        }
      });
    log.info("[ECSHealthCheckValidator] end validating ECS Health Check");
  }
}