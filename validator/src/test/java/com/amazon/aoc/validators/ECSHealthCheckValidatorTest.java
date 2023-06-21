package com.amazon.aoc.validators;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ECSContext;
import com.amazon.aoc.services.TaskService;
import com.amazonaws.services.ecs.model.Container;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ECSHealthCheckValidatorTest {
    private ECSHealthCheckValidator ecsHealthCheckValidator;

    private TaskService taskService;

    @Before
    public void setUp() {
        taskService = mock(TaskService.class);
        ecsHealthCheckValidator = new ECSHealthCheckValidator(taskService, 1);
    }

    @Test
    public void testECSHealthCheckValidatorSuccess() throws Exception {
        ecsHealthCheckValidator.init(initContext(), null, null, null);
        Container container = prepareContainer("HEALTHY");
        Task task = prepareTask(container);
        DescribeTasksResult result = prepareDescribeTasksResult(task);
        List<String> anyArnList = new ArrayList<String>(Arrays.asList("anyArn"));
        doReturn(anyArnList).when(taskService).listTasks("DummyClusterArn");
        doReturn(result).when(taskService).describeTask(anyArnList, "DummyClusterArn");
        ecsHealthCheckValidator.validate();
    }

    @Test(expected = BaseException.class)
    public void testECSHealthCheckWhenDescribeTaskResultIsNull() throws Exception {
        ecsHealthCheckValidator.init(initContext(), null, null, null);
        DescribeTasksResult result = prepareDescribeTasksResult(null);
        List<String> anyArnList = new ArrayList<String>(Arrays.asList("anyArn"));
        doReturn(result).when(taskService).describeTask(anyArnList, "DummyClusterArn");
        ecsHealthCheckValidator.validate();
    }

    @Test(expected = BaseException.class)
    public void testECSHealthCheckWhenTaskHasZeroContainer() throws Exception {
        ecsHealthCheckValidator.init(initContext(), null, null, null);
        Task task = prepareTask(null);
        DescribeTasksResult result = prepareDescribeTasksResult(task);
        List<String> anyArnList = new ArrayList<String>(Arrays.asList("anyArn"));
        doReturn(result).when(taskService).describeTask(anyArnList, "DummyClusterArn");
        ecsHealthCheckValidator.validate();
    }

    @Test(expected = BaseException.class)
    public void testECSHealthCheckWhenContainerStatusIsUnknown() throws Exception {
        ecsHealthCheckValidator.init(initContext(), null, null, null);
        Container container = prepareContainer("UNKNOWN");
        Task task = prepareTask(container);
        DescribeTasksResult result = prepareDescribeTasksResult(task);
        List<String> anyArnList = new ArrayList<String>(Arrays.asList("anyArn"));
        doReturn(result).when(taskService).describeTask(anyArnList, "DummyClusterArn");
        ecsHealthCheckValidator.validate();
    }

    private Container prepareContainer(String status) {
        Container container = new Container();
        container.setName("aoc-collector");
        container.setHealthStatus(status);
        return container;
    }

    private DescribeTasksResult prepareDescribeTasksResult(Task task) {
        final DescribeTasksResult result = new DescribeTasksResult();
        List<Task> tasks = new ArrayList<>();
        if (task != null) {
            tasks.add(task);
        }
        result.setTasks(tasks);
        return result;
    }

    private Task prepareTask(Container container) {
        Task task = new Task();
        List<Container> containers = new ArrayList<>();
        if (container != null) {
            containers.add(container);
        }
        task.setContainers(containers);
        task.setTaskDefinitionArn("DummyTaskDefArn");
        task.setLastStatus("RUNNING");
        return task;
    }

    private Context initContext() {
        // fake vars
        String testingId = "fakedTesingId";
        String region = "us-west-2";

        // faked context
        Context context = new Context(testingId, region, false, true);
        ECSContext ecsContext = new ECSContext();
        ecsContext.setEcsClusterArn("DummyClusterArn");
        ecsContext.setEcsTaskDefArn("DummyTaskDefArn");
        context.setEcsContext(ecsContext);
        return context;
    }
}
