package co.orquex.sagas.core.stage;

import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;
import static co.orquex.sagas.core.fixture.TaskFixture.getTask;
import static co.orquex.sagas.core.fixture.TaskFixture.getTaskWithCircuitBreakerConfig;
import static co.orquex.sagas.core.fixture.TaskFixture.getTaskWithRetryConfig;
import static co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy.RESULT;
import static co.orquex.sagas.domain.task.TaskConfiguration.DEFAULT_EXECUTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.orquex.sagas.core.resilience.CircuitBreakerStateManager;
import co.orquex.sagas.core.resilience.RetryStateManager;
import co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy;
import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.api.registry.Registry;
import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Evaluation;
import co.orquex.sagas.domain.task.Task;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationProcessingStrategyTest {

  @Mock Registry<TaskExecutor> taskExecutorRegistry;
  @Mock TaskRepository taskRepository;
  @Mock TaskExecutor taskExecutor;
  @Mock RetryStateManager retryStateManager;
  @Mock CircuitBreakerStateManager circuitBreakerStateManager;

  EvaluationProcessingStrategy strategy;
  ExecutionRequest executionRequest;
  String transactionId;

  @BeforeEach
  void setUp() {
    strategy =
        new EvaluationProcessingStrategy(
            taskExecutorRegistry, taskRepository, retryStateManager, circuitBreakerStateManager);
    executionRequest =
        new ExecutionRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    when(taskExecutorRegistry.get(DEFAULT_EXECUTOR)).thenReturn(Optional.of(taskExecutor));
    transactionId = UUID.randomUUID().toString();
  }

  @Test
  void shouldReturnDefaultOutgoingWhenConditionsNotMatched() {
    final var simpleTask = getTask("simple-evaluation-task");
    when(taskExecutor.execute(anyString(), any(Task.class), any(ExecutionRequest.class)))
        .thenReturn(Map.of(RESULT, false));
    when(taskRepository.findById("simple-evaluation-task")).thenReturn(Optional.of(simpleTask));
    final var evaluation = readValue("stage-evaluation-simple.json", Evaluation.class);
    final var response = strategy.process(transactionId, evaluation, executionRequest);
    assertThat(response).isNotNull();
    assertThat(response.outgoing()).isEqualTo(evaluation.getDefaultOutgoing());
  }

  @Test
  void shouldReturnTheTaskOutgoingWhenResultTrue() {
    final var simpleTask = getTask("simple-evaluation-task");
    when(taskExecutor.execute(anyString(), any(Task.class), any(ExecutionRequest.class)))
        .thenReturn(Map.of(RESULT, false))
        .thenReturn(Map.of(RESULT, true));
    when(taskRepository.findById("simple-evaluation-task")).thenReturn(Optional.of(simpleTask));
    final var evaluation = readValue("stage-evaluation-simple.json", Evaluation.class);
    final var response = strategy.process(transactionId, evaluation, executionRequest);
    assertThat(response).isNotNull();
    assertThat(response.outgoing()).isEqualTo("outgoing-test-2");
  }

  @Test
  void shouldExecuteEvaluationTaskWithRetryConfiguration() {
    // Given - Evaluation task with retry configuration using the correct task ID from JSON
    final var taskWithRetry =
        getTaskWithRetryConfig("simple-evaluation-task", 3, Duration.ofSeconds(1));

    // Mock dependencies - using the task ID that matches the JSON configuration
    when(taskRepository.findById("simple-evaluation-task")).thenReturn(Optional.of(taskWithRetry));

    // Mock successful execution after retry logic - returns true, so first condition matches
    when(taskExecutor.execute(anyString(), eq(taskWithRetry), any(ExecutionRequest.class)))
        .thenReturn(Map.of(RESULT, true));

    // Load evaluation configuration
    final var evaluation = readValue("stage-evaluation-simple.json", Evaluation.class);

    // When
    var response = strategy.process(transactionId, evaluation, executionRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.outgoing())
        .isEqualTo("outgoing-test-1"); // First condition matches when result is true

    // Verify retry-enabled task executor was called
    verify(taskExecutor).execute(anyString(), eq(taskWithRetry), any(ExecutionRequest.class));
  }

  @Test
  void shouldFailEvaluationWhenRetryAttemptsExhausted() {
    // Given - Evaluation task with retry configuration that fails persistently
    final var taskWithRetry =
        getTaskWithRetryConfig("simple-evaluation-task", 2, Duration.ofSeconds(1));

    // Mock dependencies - using the task ID that matches the JSON configuration
    when(taskRepository.findById("simple-evaluation-task")).thenReturn(Optional.of(taskWithRetry));

    // Mock persistent failure
    when(taskExecutor.execute(anyString(), eq(taskWithRetry), any(ExecutionRequest.class)))
        .thenThrow(new RuntimeException("Evaluation task persistent failure"));

    // Load evaluation configuration
    final var evaluation = readValue("stage-evaluation-simple.json", Evaluation.class);

    // When & Then
    assertThatThrownBy(() -> strategy.process(transactionId, evaluation, executionRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Evaluation task persistent failure");

    // Verify task executor was called
    verify(taskExecutor).execute(anyString(), eq(taskWithRetry), any(ExecutionRequest.class));
  }

  @Test
  void shouldExecuteFallbackWhenEvaluationCircuitBreakerIsOpen() {
    // Given - Evaluation task with circuit breaker configuration and fallback
    final var taskWithCBAndFallback =
        getTaskWithCircuitBreakerConfig("simple-evaluation-task", 2, Duration.ofSeconds(15), 1);
    final var fallbackTask = getTask("fallback-task-id");

    // Mock dependencies - using the task ID that matches the JSON configuration
    when(taskRepository.findById("simple-evaluation-task"))
        .thenReturn(Optional.of(taskWithCBAndFallback));
    when(taskRepository.findById("fallback-task-id")).thenReturn(Optional.of(fallbackTask));

    // Set circuit breaker state to OPEN to trigger fallback execution
    when(circuitBreakerStateManager.getState("simple-evaluation-task"))
        .thenReturn(co.orquex.sagas.core.resilience.CircuitBreakerState.State.OPEN);

    // Mock opened timestamp to be recent so wait duration hasn't expired (circuit stays OPEN)
    when(circuitBreakerStateManager.getOpenedTimestamp("simple-evaluation-task"))
        .thenReturn(
            java.time.Instant.now()
                .minusSeconds(5)); // Opened 5 seconds ago, break duration is 15 seconds

    // Mock fallback execution - fallback returns false for both conditions, so default outgoing
    // should be used
    when(taskExecutor.execute(anyString(), eq(fallbackTask), any(ExecutionRequest.class)))
        .thenReturn(Map.of(RESULT, false));

    // Load evaluation configuration
    final var evaluation = readValue("stage-evaluation-simple.json", Evaluation.class);

    // When
    var response = strategy.process(transactionId, evaluation, executionRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.outgoing())
        .isEqualTo(evaluation.getDefaultOutgoing()); // Fallback returned false for both conditions

    // Verify fallback task was executed twice (once for each condition in the JSON file)
    verify(taskExecutor, times(2))
        .execute(anyString(), eq(fallbackTask), any(ExecutionRequest.class));
    verify(taskExecutor, never())
        .execute(anyString(), eq(taskWithCBAndFallback), any(ExecutionRequest.class));
  }
}
