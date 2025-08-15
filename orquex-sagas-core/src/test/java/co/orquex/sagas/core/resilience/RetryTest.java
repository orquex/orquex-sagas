package co.orquex.sagas.core.resilience;

import static co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy.RESULT;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.resilience.exception.MaxRetriesExceededException;
import co.orquex.sagas.core.resilience.impl.RetryImpl;
import co.orquex.sagas.core.task.TaskExecutionContext;
import co.orquex.sagas.core.task.TaskExecutorService;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.task.TaskProcessor;
import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Retry Implementation Tests")
class RetryTest {

  @Mock private RetryStateManager stateManager;
  @Mock private TaskExecutorService taskExecutorService;
  @Mock private TaskProcessor successPolicyTask;

  private static final String TASK_NAME = "test-task";
  private static final String TRANSACTION_ID = "tx-123";
  private static final String FLOW_ID = "flow-456";
  private static final String CORRELATION_ID = "corr-789";

  @BeforeEach
  void setUp() {
    lenient().when(stateManager.incrementAndGet(TASK_NAME)).thenReturn(1L, 2L, 3L, 4L, 5L);
  }

  @Nested
  @DisplayName("Successful Execution Scenarios")
  class SuccessfulExecutionScenarios {

    @Test
    @DisplayName("Should succeed on first attempt when supplier returns valid result")
    void shouldSucceedOnFirstAttempt() {
      // Given: A retry configuration with basic settings
      var configuration = createBasicRetryConfiguration();
      var retry = new RetryImpl(configuration);
      final Map<String, Serializable> expectedResult = Map.of("key", "value");
      final Supplier<Map<String, Serializable>> supplier = () -> expectedResult;

      // When: Calling the retry mechanism
      var result = retry.call(supplier);

      // Then: Should return the result immediately without retries
      assertThat(result).isEqualTo(expectedResult);
      verify(stateManager).incrementAndGet(TASK_NAME);
      verifyNoInteractions(taskExecutorService);
    }

    @Test
    @DisplayName("Should succeed after some failures when supplier eventually returns valid result")
    void shouldSucceedAfterFailures() {
      // Given: A supplier that fails twice then succeeds
      lenient().when(stateManager.value(TASK_NAME)).thenReturn(1L, 2L, 3L);
      var configuration = createBasicRetryConfiguration();
      var retry = new RetryImpl(configuration);
      final Map<String, Serializable> expectedResult = Map.of("success", "true");

      var attemptCounter = new AtomicInteger(0);
      Supplier<Map<String, Serializable>> supplier =
          () -> {
            if (attemptCounter.incrementAndGet() <= 2) {
              throw new WorkflowException("Temporary failure");
            }
            return expectedResult;
          };

      when(stateManager.incrementAndGet(TASK_NAME)).thenReturn(1L, 2L, 3L);

      // When: Calling the retry mechanism
      var result = retry.call(supplier);

      // Then: Should succeed after retries
      assertThat(result).isEqualTo(expectedResult);
      verify(stateManager, times(3)).incrementAndGet(TASK_NAME);
    }

    @Test
    @DisplayName("Should succeed when result validation passes")
    void shouldSucceedWhenResultValidationPasses() {
      // Given: A retry configuration with result validation
      var configuration = createRetryConfigurationWithResultValidation();
      var retry = new RetryImpl(configuration);
      final Map<String, Serializable> supplierResult = Map.of("data", "test");
      final Map<String, Serializable> validationResponse = Map.of(RESULT, Boolean.TRUE);

      Supplier<Map<String, Serializable>> supplier = () -> supplierResult;

      when(taskExecutorService.executeTask(
              eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class)))
          .thenReturn(validationResponse);

      // When: Calling the retry mechanism
      var result = retry.call(supplier);

      // Then: Should succeed after validation
      assertThat(result).isEqualTo(supplierResult);
      verify(taskExecutorService)
          .executeTask(eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class));
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("Should throw WorkflowException immediately when retryWorkflowException is false")
    void shouldNotRetryWhenRetryWorkflowExceptionIsFalse() {
      // Given: Configuration with retryWorkflowException = false
      var configuration =
          new RetryConfiguration(
              TASK_NAME,
              3,
              Duration.ofMillis(10),
              false,
              successPolicyTask,
              stateManager,
              getTaskExecutionContext());

      var retry = new RetryImpl(configuration);
      var expectedException = new WorkflowException("Should not retry");

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw expectedException;
          };

      // When & Then: Should throw immediately without a retry
      assertThatThrownBy(() -> retry.call(supplier)).isEqualTo(expectedException);

      verify(stateManager).incrementAndGet(TASK_NAME);
    }

    @Test
    @DisplayName("Should exhaust retries when result validation consistently fails")
    void shouldExhaustRetriesWhenResultValidationFails() {
      // Given: Configuration with result validation that always fails
      var configuration = createRetryConfigurationWithResultValidation();
      var retry = new RetryImpl(configuration);
      final Map<String, Serializable> supplierResult = Map.of("data", "test");
      final Map<String, Serializable> validationResponse = Map.of(RESULT, Boolean.FALSE);

      Supplier<Map<String, Serializable>> supplier = () -> supplierResult;

      when(stateManager.incrementAndGet(TASK_NAME)).thenReturn(1L, 2L, 3L, 4L);
      when(taskExecutorService.executeTask(
              eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class)))
          .thenReturn(validationResponse);

      // When & Then: Should throw MaxRetriesExceededException
      assertThatThrownBy(() -> retry.call(supplier))
          .isInstanceOf(MaxRetriesExceededException.class);

      verify(taskExecutorService, atLeast(1))
          .executeTask(eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class));
    }

    @Test
    @DisplayName("Should handle validation task exception gracefully")
    void shouldHandleValidationTaskException() {
      // Given: Configuration where a validation task throws an exception
      var configuration = createRetryConfigurationWithResultValidation();
      var retry = new RetryImpl(configuration);
      final Map<String, Serializable> supplierResult = Map.of("data", "test");

      Supplier<Map<String, Serializable>> supplier = () -> supplierResult;

      when(stateManager.incrementAndGet(TASK_NAME)).thenReturn(1L, 2L, 3L, 4L);
      when(taskExecutorService.executeTask(
              eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class)))
          .thenThrow(new WorkflowException("Validation failed"));

      // When & Then: Should exhaust retries
      assertThatThrownBy(() -> retry.call(supplier))
          .isInstanceOf(MaxRetriesExceededException.class);
    }
  }

  @Nested
  @DisplayName("Configuration Scenarios")
  class ConfigurationScenarios {

    @Test
    @DisplayName("Should throw IllegalArgumentException for null configuration")
    void shouldThrowExceptionForNullConfiguration() {
      // When & Then: Should throw IllegalArgumentException
      assertThatThrownBy(() -> new RetryImpl(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Retry configuration cannot be null");
    }

    @Test
    @DisplayName("Should respect wait duration between retries")
    void shouldRespectWaitDuration() {
      // Given: Configuration with specific wait duration
      var waitDuration = Duration.ofMillis(100);
      var configuration =
          new RetryConfiguration(
              TASK_NAME,
              2,
              waitDuration,
              true,
              successPolicyTask,
              stateManager,
              getTaskExecutionContext());

      var retry = new RetryImpl(configuration);
      var attemptCounter = new AtomicInteger(0);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            if (attemptCounter.incrementAndGet() == 1) {
              throw new WorkflowException("First failure");
            }
            return Map.of("success", "true");
          };

      when(stateManager.value(TASK_NAME)).thenReturn(1L, 2L, 3L);
      when(stateManager.incrementAndGet(TASK_NAME)).thenReturn(1L, 2L, 3L);

      // When: Measure execution time
      // Then: Should have waited at least the specified duration
      await()
          .atMost(500, TimeUnit.MILLISECONDS)
          .untilAsserted(
              () ->
                  assertThatThrownBy(() -> retry.call(supplier))
                      .isInstanceOf(WorkflowException.class));
    }

    @Test
    @DisplayName("Should handle different max attempt values correctly")
    void shouldHandleDifferentMaxAttempts() {
      // Given: Configuration with maxAttempts = 1
      var configuration =
          new RetryConfiguration(
              TASK_NAME,
              1,
              Duration.ofMillis(1),
              true,
              successPolicyTask,
              stateManager,
              getTaskExecutionContext());

      var retry = new RetryImpl(configuration);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Always fails");
          };

      when(stateManager.incrementAndGet(TASK_NAME)).thenReturn(1L, 2L);

      // When & Then: Should fail after a single attempt
      assertThatThrownBy(() -> retry.call(supplier))
          .isInstanceOf(WorkflowException.class)
          .hasMessage("Always fails");

      verify(stateManager).incrementAndGet(TASK_NAME);
    }
  }

  @Nested
  @DisplayName("Interruption Scenarios")
  class InterruptionScenarios {

    @Test
    @DisplayName("Should handle thread interruption during wait and restore interrupt status")
    void shouldHandleThreadInterruption() {
      // Given: A retry that will be interrupted during the wait
      var configuration = createBasicRetryConfiguration();
      var retry = new RetryImpl(configuration);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Trigger retry");
          };
      when(stateManager.value(TASK_NAME)).thenReturn(1L);
      when(stateManager.incrementAndGet(TASK_NAME)).thenReturn(1L);

      // When: Interrupt the thread during execution
      Thread currentThread = Thread.currentThread();
      try (final var scheduler = Executors.newSingleThreadScheduledExecutor(); ) {
        scheduler.schedule(
            currentThread::interrupt, 50, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Then: Should throw WorkflowException and maintain interrupt status
        assertThatThrownBy(() -> retry.call(supplier))
            .isInstanceOf(WorkflowException.class)
            .hasMessageContaining("Trigger retry");

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("State Management Scenarios")
  class StateManagementScenarios {

    @Test
    @DisplayName("Should increment retry state correctly")
    void shouldIncrementRetryStateCorrectly() {
      // Given: Configuration and successful supplier
      var configuration = createBasicRetryConfiguration();
      var retry = new RetryImpl(configuration);

      Supplier<Map<String, Serializable>> supplier = () -> Map.of("result", "success");

      // When: Call retry
      retry.call(supplier);

      // Then: Should increment state once
      verify(stateManager).incrementAndGet(TASK_NAME);
    }

    @Test
    @DisplayName("Should query final state value for exception message")
    void shouldQueryFinalStateValue() {
      // Given: Configuration that will exhaust retries
      var configuration = createRetryConfigurationWithResultValidation();
      var retry = new RetryImpl(configuration);
      final Map<String, Serializable> supplierResult = Map.of("data", "test");
      final Map<String, Serializable> validationResponse = Map.of(RESULT, Boolean.FALSE);

      Supplier<Map<String, Serializable>> supplier = () -> supplierResult;

      when(stateManager.incrementAndGet(TASK_NAME)).thenReturn(1L, 2L, 3L, 4L);
      when(stateManager.value(TASK_NAME)).thenReturn(3L);
      when(taskExecutorService.executeTask(
              eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class)))
          .thenReturn(validationResponse);

      // When & Then: Should use state value in an exception message
      assertThatThrownBy(() -> retry.call(supplier))
          .isInstanceOf(MaxRetriesExceededException.class)
          .hasMessageContaining("(3)");

      verify(stateManager, times(4)).incrementAndGet(TASK_NAME);
    }
  }

  // Helper methods for creating configurations
  private RetryConfiguration createBasicRetryConfiguration() {
    return new RetryConfiguration(
        TASK_NAME, 3L, Duration.ofMillis(10), true, null, stateManager, getTaskExecutionContext());
  }

  private RetryConfiguration createRetryConfigurationWithResultValidation() {
    when(successPolicyTask.task()).thenReturn("validation-task");
    when(successPolicyTask.metadata()).thenReturn(Map.of("__expression", "a == 1"));

    return new RetryConfiguration(
        TASK_NAME,
        3L,
        Duration.ofMillis(10),
        true,
        successPolicyTask,
        stateManager,
        getTaskExecutionContext());
  }

  private TaskExecutionContext getTaskExecutionContext() {
    return new TaskExecutionContext(TRANSACTION_ID, FLOW_ID, CORRELATION_ID, taskExecutorService);
  }
}
