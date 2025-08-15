package co.orquex.sagas.core.stage;

import static co.orquex.sagas.core.fixture.ActivityFixture.getSimpleActivity;
import static co.orquex.sagas.core.fixture.ActivityTaskFixture.getSimpleActivityTask;
import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static co.orquex.sagas.core.fixture.TaskFixture.getTask;
import static co.orquex.sagas.core.fixture.TaskFixture.getTaskWithBothResilienceConfigs;
import static co.orquex.sagas.core.fixture.TaskFixture.getTaskWithCircuitBreakerConfig;
import static co.orquex.sagas.core.fixture.TaskFixture.getTaskWithRetryConfig;
import static co.orquex.sagas.domain.task.TaskConfiguration.DEFAULT_EXECUTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.orquex.sagas.core.resilience.CircuitBreakerStateManager;
import co.orquex.sagas.core.resilience.RetryStateManager;
import co.orquex.sagas.core.stage.strategy.impl.ActivityProcessingStrategy;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.task.Task;
import co.orquex.sagas.domain.transaction.Compensation;
import java.time.Duration;
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
  @Mock RetryStateManager retryStateManager;
  @Mock CircuitBreakerStateManager circuitBreakerStateManager;
  @Mock Consumer<Compensation> compensationConsumer;

  ActivityProcessingStrategy strategy;
  ExecutionRequest executionRequest;
  String transactionId;

  @BeforeEach
  void setUp() {
    strategy =
        new ActivityProcessingStrategy(
            taskExecutorRegistry,
            taskRepository,
            retryStateManager,
            circuitBreakerStateManager,
            compensationConsumer);
    executionRequest =
        new ExecutionRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    transactionId = UUID.randomUUID().toString();
  }

  @Test
  void shouldProcessActivitySyncTasks() {
    final var simpleTask = getTask("simple-task");
    final var preTask = getTask("pre-impl-id");
    final var postTask = getTask("post-impl-id");
    // Task executor stateManager
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
    // Task executor stateManager
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.empty());
    when(taskRepository.findById("single-task")).thenReturn(Optional.of(getTask("single-task")));
    final var activity = readValue("stage-activity-single-task.json", Activity.class);
    assertThatThrownBy(() -> strategy.process(transactionId, activity, executionRequest))
        .isInstanceOf(WorkflowException.class)
        .hasMessage("Task executor 'default' not registered");
  }

  @Test
  void shouldProcessActivityParallelTasks() {
    // Task executor stateManager
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
    // Task executor stateManager
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
    // Task executor stateManager
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

  @Test
  void shouldExecuteTaskWithRetryConfiguration() {
    // Given - Task with retry configuration
    final var taskWithRetry = getTaskWithRetryConfig("retry-task", 3, Duration.ofSeconds(1));

    // Mock dependencies
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    when(taskRepository.findById("retry-task")).thenReturn(Optional.of(taskWithRetry));

    // Mock successful execution after retry logic
    when(taskExecutor.execute(anyString(), eq(taskWithRetry), any(ExecutionRequest.class)))
        .thenReturn(Map.of("retry-task", "success"));

    // Create activity with retry task
    final var activity =
        getSimpleActivity(
            "retry-activity", List.of(getSimpleActivityTask("retry-task")), false, true);

    // When
    var stageResponse = strategy.process(transactionId, activity, executionRequest);

    // Then
    assertThat(stageResponse).isNotNull();
    assertThat(stageResponse.payload())
        .isNotNull()
        .hasSize(1)
        .containsEntry("retry-task", "success");

    // Verify task executor was called
    verify(taskExecutor).execute(anyString(), eq(taskWithRetry), any(ExecutionRequest.class));
  }

  @Test
  void shouldExecuteTaskWithCircuitBreakerConfiguration() {
    // Given - Task with circuit breaker configuration
    final var taskWithCB = getTaskWithCircuitBreakerConfig("cb-task", 2, Duration.ofSeconds(10), 1);

    // Mock dependencies
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    when(taskRepository.findById("cb-task")).thenReturn(Optional.of(taskWithCB));

    // Mock successful execution
    when(taskExecutor.execute(anyString(), eq(taskWithCB), any(ExecutionRequest.class)))
        .thenReturn(Map.of("cb-task", "success"));

    // Create activity with circuit breaker task
    final var activity =
        getSimpleActivity("cb-activity", List.of(getSimpleActivityTask("cb-task")), false, true);

    // When
    var stageResponse = strategy.process(transactionId, activity, executionRequest);

    // Then
    assertThat(stageResponse).isNotNull();
    assertThat(stageResponse.payload()).isNotNull().hasSize(1).containsEntry("cb-task", "success");

    // Verify task executor was called
    verify(taskExecutor).execute(anyString(), eq(taskWithCB), any(ExecutionRequest.class));
  }

  @Test
  void shouldExecuteTaskWithBothRetryAndCircuitBreakerConfiguration() {
    // Given - Task with both retry and circuit breaker configurations
    final var taskWithBoth =
        getTaskWithBothResilienceConfigs(
            "resilient-task", 3, Duration.ofSeconds(1), 2, Duration.ofSeconds(10), 1);

    // Mock dependencies
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    when(taskRepository.findById("resilient-task")).thenReturn(Optional.of(taskWithBoth));

    // Mock successful execution
    when(taskExecutor.execute(anyString(), eq(taskWithBoth), any(ExecutionRequest.class)))
        .thenReturn(Map.of("resilient-task", "success"));

    // Create activity with resilient task
    final var activity =
        getSimpleActivity(
            "resilient-activity", List.of(getSimpleActivityTask("resilient-task")), false, true);

    // When
    var stageResponse = strategy.process(transactionId, activity, executionRequest);

    // Then
    assertThat(stageResponse).isNotNull();
    assertThat(stageResponse.payload())
        .isNotNull()
        .hasSize(1)
        .containsEntry("resilient-task", "success");

    // Verify task executor was called
    verify(taskExecutor).execute(anyString(), eq(taskWithBoth), any(ExecutionRequest.class));
  }

  @Test
  void shouldFailWhenRetryAttemptsExhausted() {
    // Given - Task with retry configuration
    final var taskWithRetry = getTaskWithRetryConfig("failing-task", 2, Duration.ofSeconds(1));

    // Mock dependencies
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    when(taskRepository.findById("failing-task")).thenReturn(Optional.of(taskWithRetry));

    // Mock persistent failure
    when(taskExecutor.execute(anyString(), eq(taskWithRetry), any(ExecutionRequest.class)))
        .thenThrow(new RuntimeException("Persistent failure"));

    // Create activity with failing task
    final var activity =
        getSimpleActivity(
            "failing-activity", List.of(getSimpleActivityTask("failing-task")), false, true);

    // When & Then
    assertThatThrownBy(() -> strategy.process(transactionId, activity, executionRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Persistent failure");

    // Verify task executor was called
    verify(taskExecutor).execute(anyString(), eq(taskWithRetry), any(ExecutionRequest.class));
  }

  @Test
  void shouldExecuteFallbackWhenCircuitBreakerIsOpen() {
    // Given - Task with circuit breaker configuration and fallback
    final var taskWithCBAndFallback =
        getTaskWithCircuitBreakerConfig("cb-fallback-task", 2, Duration.ofSeconds(15), 1);
    final var fallbackTask = getTask("fallback-task-id");

    // Mock dependencies
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    when(taskRepository.findById("cb-fallback-task"))
        .thenReturn(Optional.of(taskWithCBAndFallback));
    when(taskRepository.findById("fallback-task-id")).thenReturn(Optional.of(fallbackTask));

    // Set circuit breaker state to OPEN to trigger fallback execution
    when(circuitBreakerStateManager.getState("cb-fallback-task"))
        .thenReturn(co.orquex.sagas.core.resilience.CircuitBreakerState.State.OPEN);
    // Mock opened timestamp to be recent so wait duration hasn't expired (circuit stays OPEN)
    when(circuitBreakerStateManager.getOpenedTimestamp("cb-fallback-task"))
        .thenReturn(
            java.time.Instant.now()
                .minusSeconds(5)); // Opened 5 seconds ago, break duration is 15 seconds
    // Mock fallback execution
    when(taskExecutor.execute(anyString(), eq(fallbackTask), any(ExecutionRequest.class)))
        .thenReturn(Map.of("result", "fallback-result"));

    // Create activity with circuit breaker task
    final var activity =
        getSimpleActivity(
            "cb-fallback-activity",
            List.of(getSimpleActivityTask("cb-fallback-task")),
            false,
            true);

    // When
    var stageResponse = strategy.process(transactionId, activity, executionRequest);

    // Then
    assertThat(stageResponse).isNotNull();
    assertThat(stageResponse.payload())
        .isNotNull()
        .hasSize(1)
        .containsEntry("result", "fallback-result");

    // Verify fallback task was executed instead of the original task
    verify(taskExecutor).execute(anyString(), eq(fallbackTask), any(ExecutionRequest.class));
    verify(taskExecutor, never())
        .execute(anyString(), eq(taskWithCBAndFallback), any(ExecutionRequest.class));
  }
}
