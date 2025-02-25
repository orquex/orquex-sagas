package co.orquex.sagas.core.stage;

import static co.orquex.sagas.core.fixture.ActivityFixture.getSimpleActivity;
import static co.orquex.sagas.core.fixture.ActivityTaskFixture.getSimpleActivityTask;
import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static co.orquex.sagas.core.fixture.TaskFixture.getTask;
import static co.orquex.sagas.domain.task.TaskConfiguration.DEFAULT_EXECUTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.orquex.sagas.core.stage.strategy.impl.ActivityProcessingStrategy;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.task.Task;
import co.orquex.sagas.domain.transaction.Compensation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityProcessingStrategyTest {

  @Mock Registry<TaskExecutor> taskExecutorRegistry;
  @Mock TaskRepository taskRepository;
  @Mock TaskExecutor taskExecutor;

  ActivityProcessingStrategy strategy;
  ExecutionRequest executionRequest;
  String transactionId;

  @Mock Consumer<Compensation> compensationConsumer;

  @BeforeEach
  void setUp() {
    strategy =
        new ActivityProcessingStrategy(taskExecutorRegistry, taskRepository, compensationConsumer);
    executionRequest =
        new ExecutionRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    transactionId = UUID.randomUUID().toString();
  }

  @Test
  void shouldProcessActivitySyncTasks() {
    final var simpleTask = getTask("simple-task");
    final var preTask = getTask("pre-impl-id");
    final var postTask = getTask("post-impl-id");
    // Task executor registry
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    // Task repository
    when(taskRepository.findById("simple-task")).thenReturn(Optional.of(simpleTask));
    when(taskRepository.findById("pre-impl-id")).thenReturn(Optional.of(preTask));
    when(taskRepository.findById("post-impl-id")).thenReturn(Optional.of(postTask));
    // Task executor
    when(taskExecutor.execute(anyString(), eq(simpleTask), any(ExecutionRequest.class)))
        .thenReturn(Map.of("simple", "task"));
    when(taskExecutor.execute(anyString(), eq(preTask), any(ExecutionRequest.class)))
        .thenReturn(Map.of("pre", "task"));
    when(taskExecutor.execute(anyString(), eq(postTask), any(ExecutionRequest.class)))
        .thenReturn(Map.of("post", "task"));
    final var activity = readValue("stage-activity-simple.json", Activity.class);

    var stageResponse = strategy.process(transactionId, activity, executionRequest);
    assertThat(stageResponse).isNotNull();
    assertThat(stageResponse.payload()).isNotNull().hasSize(1).containsEntry("post", "task");
    verify(compensationConsumer).accept(any(Compensation.class));
  }

  @Test
  void shouldThrowExceptionWhenTaskNotFound() {
    when(taskRepository.findById("single-task")).thenReturn(Optional.empty());
    final var activity = readValue("stage-activity-single-task.json", Activity.class);
    assertThatThrownBy(() -> strategy.process(transactionId, activity, executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Task 'single-task' not found");
  }

  @Test
  void shouldThrowExceptionWhenActivityTaskExecutorNotFound() {
    // Task executor registry
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.empty());
    when(taskRepository.findById("single-task")).thenReturn(Optional.of(getTask("single-task")));
    final var activity = readValue("stage-activity-single-task.json", Activity.class);
    assertThatThrownBy(() -> strategy.process(transactionId, activity, executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Task executor 'default' not registered");
  }

  @Test
  void shouldProcessActivityParallelTasks() {
    // Task executor registry
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    // Task repository
    when(taskRepository.findById(anyString()))
        .thenReturn(Optional.of(getTask("task-1")))
        .thenReturn(Optional.of(getTask("task-2")));
    // Task executor
    when(taskExecutor.execute(anyString(), any(Task.class), any(ExecutionRequest.class)))
        .thenReturn(Map.of("task-1", "1"))
        .thenReturn(Map.of("task-2", "2"))
        .thenReturn(Map.of("task-3", "3"))
        .thenReturn(Map.of("task-4", "4"));

    final var activity = readValue("stage-activity-parallel-task.json", Activity.class);
    final var stageResponse = strategy.process(transactionId, activity, executionRequest);
    assertThat(stageResponse).isNotNull();
    assertThat(stageResponse.payload())
        .isNotNull()
        .hasSize(4)
        .containsEntry("task-1", "1")
        .containsEntry("task-2", "2")
        .containsEntry("task-3", "3")
        .containsEntry("task-4", "4");
  }

  @Test
  void shouldThrowExceptionWhenActivityParallelTaskExecutorNotFound() {
    // Task executor registry
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR))
        .thenReturn(Optional.of(taskExecutor))
        .thenReturn(Optional.empty());
    when(taskRepository.findById(anyString()))
        .thenReturn(Optional.of(getTask("task-1")))
        .thenReturn(Optional.of(getTask("task-2")));
    final var activity = readValue("stage-activity-parallel-task.json", Activity.class);
    assertThatThrownBy(() -> strategy.process(transactionId, activity, executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Task executor 'default' not registered");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldContinueActivityTaskExecutionWhenAllOrNothingIsFalse(boolean parallel) {
    // Task executor registry
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    // Task repository
    when(taskRepository.findById(anyString()))
        .thenReturn(Optional.of(getTask("task-1")))
        .thenReturn(Optional.of(getTask("task-2")))
        .thenReturn(Optional.of(getTask("task-3")))
        .thenReturn(Optional.of(getTask("task-4")));
    // Task executor
    when(taskExecutor.execute(anyString(), any(Task.class), any(ExecutionRequest.class)))
        .thenReturn(Map.of("task-1", "1"))
        .thenReturn(Map.of("task-2", "2"))
        .thenThrow(new WorkflowException("Task 'task-3' failed"))
        .thenReturn(Map.of("task-4", "4"));
    // Parametrized parallel ActivityTask with allOrNothing = false
    final var activity =
        getSimpleActivity(
            "activity-1",
            List.of(
                getSimpleActivityTask("task-1"),
                getSimpleActivityTask("task-2"),
                getSimpleActivityTask("task-3"),
                getSimpleActivityTask("task-4")),
            parallel,
            false);
    final var stageResponse = strategy.process(transactionId, activity, executionRequest);
    assertThat(stageResponse).isNotNull();
    assertThat(stageResponse.payload())
        .isNotNull()
        .hasSize(3)
        .containsEntry("task-1", "1")
        .containsEntry("task-2", "2")
        .containsEntry("task-4", "4");
  }
}
