package co.orquex.sagas.core.resilience;

import static co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy.RESULT;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.orquex.sagas.core.resilience.exception.CircuitBreakerOpenException;
import co.orquex.sagas.core.task.TaskExecutionContext;
import co.orquex.sagas.core.task.TaskExecutorService;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.task.TaskProcessor;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CircuitBreaker Implementation Tests")
class CircuitBreakerTest {

  @Mock CircuitBreakerStateManager stateManager;
  @Mock TaskExecutorService taskExecutorService;
  @Mock TaskProcessor successPolicy;
  @Mock CircuitBreakerFallback fallback;
  @Mock TaskProcessor fallbackTaskProcessor;

  static final String TASK_NAME = "test-circuit-breaker";
  static final String TRANSACTION_ID = "tx-123";
  static final String FLOW_ID = "flow-456";
  static final String CORRELATION_ID = "corr-789";
  static final Duration BREAK_DURATION = Duration.ofMillis(100);

  @BeforeEach
  void setUp() {
    // Default state setup - circuit starts in CLOSED state
    lenient().when(stateManager.getState(TASK_NAME)).thenReturn(CircuitBreakerState.State.CLOSED);
    lenient().when(stateManager.getFailureCount(TASK_NAME)).thenReturn(0L);
    lenient().when(stateManager.getSuccessCount(TASK_NAME)).thenReturn(0L);
    lenient().when(stateManager.incrementFailureCount(TASK_NAME)).thenReturn(1L, 2L, 3L, 4L, 5L);
    lenient().when(stateManager.incrementSuccessCount(TASK_NAME)).thenReturn(1L, 2L, 3L, 4L, 5L);
  }

  @Nested
  @DisplayName("CLOSED State Scenarios")
  class ClosedStateScenarios {

    @Test
    @DisplayName("Should execute successfully when supplier returns valid result")
    void shouldExecuteSuccessfullyInClosedState() {
      // Given: Basic circuit breaker configuration
      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> expectedResult = Map.of("status", "success");
      final Supplier<Map<String, Serializable>> supplier = () -> expectedResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should return result and reset failure count
      assertThat(result).isEqualTo(expectedResult);
      verify(stateManager).resetFailureCount(TASK_NAME);
      verify(stateManager, never()).incrementFailureCount(TASK_NAME);
    }

    @Test
    @DisplayName("Should increment failure count when supplier throws exception")
    void shouldIncrementFailureCountOnException() {
      // Given: Circuit breaker with fallback
      var configuration = createCircuitBreakerConfigurationWithFallback();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> fallbackResult = Map.of("fallback", "executed");

      when(fallback.execute(any(TaskExecutionContext.class))).thenReturn(fallbackResult);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Service unavailable");
          };

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should execute fallback and increment failure count
      assertThat(result).isEqualTo(fallbackResult);
      verify(stateManager).incrementFailureCount(TASK_NAME);
      verify(fallback).execute(any(TaskExecutionContext.class));
    }

    @Test
    @DisplayName("Should throw exception when no fallback configured and supplier fails")
    void shouldThrowExceptionWhenNoFallbackAndSupplierFails() {
      // Given: Circuit breaker without fallback
      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);
      var expectedException = new WorkflowException("Service down");

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw expectedException;
          };

      // When & Then: Should throw original exception and increment failure count
      assertThatThrownBy(() -> circuitBreaker.call(supplier)).isEqualTo(expectedException);

      verify(stateManager).incrementFailureCount(TASK_NAME);
    }

    @Test
    @DisplayName("Should transition to OPEN when failure threshold is exceeded")
    void shouldTransitionToOpenWhenThresholdExceeded() {
      // Given: Circuit breaker with failure threshold of 2
      when(stateManager.incrementFailureCount(TASK_NAME)).thenReturn(3L); // Exceeds threshold of 2

      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Critical failure");
          };

      // When & Then: Should transition to OPEN silently (no exception thrown)
      assertThatThrownBy(() -> circuitBreaker.call(supplier))
          .isInstanceOf(WorkflowException.class)
          .hasMessage("Critical failure");

      verify(stateManager).setState(TASK_NAME, CircuitBreakerState.State.OPEN);
      verify(stateManager).setOpenedTimestamp(eq(TASK_NAME), any(Instant.class));
      verify(stateManager).resetSuccessCount(TASK_NAME);
    }

    @Test
    @DisplayName("Should handle success policy validation failure")
    void shouldHandleSuccessPolicyValidationFailure() {
      // Given: Circuit breaker with success policy that returns false
      var configuration = createCircuitBreakerConfigurationWithSuccessPolicy();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> supplierResult = Map.of("data", "test");
      final Map<String, Serializable> policyResult = Map.of(RESULT, Boolean.FALSE);

      when(taskExecutorService.executeTask(
              eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class)))
          .thenReturn(policyResult);

      Supplier<Map<String, Serializable>> supplier = () -> supplierResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should treat as failure and increment failure count
      assertThat(result).isEqualTo(supplierResult);
      verify(stateManager).incrementFailureCount(TASK_NAME);
      verify(taskExecutorService)
          .executeTask(eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class));
    }

    @Test
    @DisplayName("Should handle success policy validation success")
    void shouldHandleSuccessPolicyValidationSuccess() {
      // Given: Circuit breaker with success policy that returns true
      var configuration = createCircuitBreakerConfigurationWithSuccessPolicy();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> supplierResult = Map.of("data", "test");
      final Map<String, Serializable> policyResult = Map.of(RESULT, Boolean.TRUE);

      when(taskExecutorService.executeTask(
              eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class)))
          .thenReturn(policyResult);

      Supplier<Map<String, Serializable>> supplier = () -> supplierResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should treat as success and reset failure count
      assertThat(result).isEqualTo(supplierResult);
      verify(stateManager).resetFailureCount(TASK_NAME);
      verify(taskExecutorService)
          .executeTask(eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class));
    }

    @Test
    @DisplayName("Should treat success policy exception as failure")
    void shouldTreatSuccessPolicyExceptionAsFailure() {
      // Given: Circuit breaker with success policy that throws exception
      var configuration = createCircuitBreakerConfigurationWithSuccessPolicy();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> supplierResult = Map.of("data", "test");

      when(taskExecutorService.executeTask(
              eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class)))
          .thenThrow(new WorkflowException("Policy execution failed"));

      Supplier<Map<String, Serializable>> supplier = () -> supplierResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should treat as failure and increment failure count
      assertThat(result).isEqualTo(supplierResult);
      verify(stateManager).incrementFailureCount(TASK_NAME);
    }
  }

  @Nested
  @DisplayName("OPEN State Scenarios")
  class OpenStateScenarios {

    @BeforeEach
    void setUpOpenState() {
      when(stateManager.getState(TASK_NAME)).thenReturn(CircuitBreakerState.State.OPEN);
      when(stateManager.getOpenedTimestamp(TASK_NAME)).thenReturn(Instant.now());
    }

    @Test
    @DisplayName("Should execute fallback when circuit is OPEN and fallback is configured")
    void shouldExecuteFallbackWhenCircuitIsOpenAndFallbackConfigured() {
      // Given: Circuit breaker in OPEN state with fallback configured
      var configuration = createCircuitBreakerConfigurationWithFallback();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> fallbackResult = Map.of("fallback", "executed");

      when(fallback.execute(any(TaskExecutionContext.class))).thenReturn(fallbackResult);

      Supplier<Map<String, Serializable>> supplier = () -> Map.of("should", "not execute");

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should execute fallback without calling supplier
      assertThat(result).isEqualTo(fallbackResult);
      verify(fallback).execute(any(TaskExecutionContext.class));
      // Supplier should never be called when circuit is OPEN
      verify(stateManager, never()).incrementFailureCount(TASK_NAME);
      verify(stateManager, never()).resetFailureCount(TASK_NAME);
    }

    @Test
    @DisplayName("Should reject calls immediately when circuit is OPEN and no fallback configured")
    void shouldRejectCallsWhenCircuitIsOpenAndNoFallbackConfigured() {
      // Given: Circuit breaker in OPEN state with no fallback
      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);

      Supplier<Map<String, Serializable>> supplier = () -> Map.of("should", "not execute");

      // When & Then: Should throw CircuitBreakerOpenException immediately
      assertThatThrownBy(() -> circuitBreaker.call(supplier))
          .isInstanceOf(CircuitBreakerOpenException.class)
          .hasMessageContaining("OPEN")
          .hasMessageContaining(TASK_NAME);

      // Supplier should never be called
      verify(stateManager, never()).incrementFailureCount(TASK_NAME);
      verify(stateManager, never()).resetFailureCount(TASK_NAME);
    }

    @Test
    @DisplayName("Should transition to HALF_OPEN when break duration expires")
    void shouldTransitionToHalfOpenWhenBreakDurationExpires() {
      // Given: Circuit breaker with expired break duration
      var pastTimestamp = Instant.now().minusMillis(BREAK_DURATION.toMillis() + 50);
      when(stateManager.getOpenedTimestamp(TASK_NAME)).thenReturn(pastTimestamp);

      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> expectedResult = Map.of("status", "success");

      Supplier<Map<String, Serializable>> supplier = () -> expectedResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should transition to HALF_OPEN and execute successfully
      assertThat(result).isEqualTo(expectedResult);
      verify(stateManager).setState(TASK_NAME, CircuitBreakerState.State.HALF_OPEN);
      verify(stateManager).resetSuccessCount(TASK_NAME);
    }
  }

  @Nested
  @DisplayName("HALF_OPEN State Scenarios")
  class HalfOpenStateScenarios {

    @BeforeEach
    void setUpHalfOpenState() {
      when(stateManager.getState(TASK_NAME)).thenReturn(CircuitBreakerState.State.HALF_OPEN);
    }

    @Test
    @DisplayName("Should increment success count on successful execution")
    void shouldIncrementSuccessCountOnSuccess() {
      // Given: Circuit breaker in HALF_OPEN state
      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> expectedResult = Map.of("status", "success");

      when(stateManager.incrementSuccessCount(TASK_NAME)).thenReturn(1L);

      Supplier<Map<String, Serializable>> supplier = () -> expectedResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should increment success count but not close circuit yet
      assertThat(result).isEqualTo(expectedResult);
      verify(stateManager).incrementSuccessCount(TASK_NAME);
      verify(stateManager, never()).setState(TASK_NAME, CircuitBreakerState.State.CLOSED);
    }

    @Test
    @DisplayName("Should transition to CLOSED when success threshold is met")
    void shouldTransitionToClosedWhenSuccessThresholdMet() {
      // Given: Circuit breaker with success threshold of 2
      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> expectedResult = Map.of("status", "success");

      when(stateManager.incrementSuccessCount(TASK_NAME)).thenReturn(2L); // Meets threshold

      Supplier<Map<String, Serializable>> supplier = () -> expectedResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should transition to CLOSED state
      assertThat(result).isEqualTo(expectedResult);
      verify(stateManager).setState(TASK_NAME, CircuitBreakerState.State.CLOSED);
      verify(stateManager).resetFailureCount(TASK_NAME);
      verify(stateManager).resetSuccessCount(TASK_NAME);
    }

    @Test
    @DisplayName("Should transition to OPEN on any failure")
    void shouldTransitionToOpenOnAnyFailure() {
      // Given: Circuit breaker in HALF_OPEN state
      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Test failure");
          };

      // When & Then: Should transition to OPEN immediately and register failure
      assertThatThrownBy(() -> circuitBreaker.call(supplier)).isInstanceOf(WorkflowException.class);

      verify(stateManager).incrementFailureCount(TASK_NAME);
      verify(stateManager).setState(TASK_NAME, CircuitBreakerState.State.OPEN);
      verify(stateManager).setOpenedTimestamp(eq(TASK_NAME), any(Instant.class));
      verify(stateManager).resetSuccessCount(TASK_NAME);
    }

    @Test
    @DisplayName("Should transition to OPEN when success policy fails")
    void shouldTransitionToOpenWhenSuccessPolicyFails() {
      // Given: Circuit breaker with success policy that returns false
      var configuration = createCircuitBreakerConfigurationWithSuccessPolicy();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> supplierResult = Map.of("data", "test");
      final Map<String, Serializable> policyResult = Map.of(RESULT, Boolean.FALSE);

      when(taskExecutorService.executeTask(
              eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class)))
          .thenReturn(policyResult);

      Supplier<Map<String, Serializable>> supplier = () -> supplierResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should transition to OPEN state and register failure
      assertThat(result).isEqualTo(supplierResult);
      verify(stateManager).incrementFailureCount(TASK_NAME);
      verify(stateManager).setState(TASK_NAME, CircuitBreakerState.State.OPEN);
      verify(stateManager).setOpenedTimestamp(eq(TASK_NAME), any(Instant.class));
    }

    @Test
    @DisplayName("Should execute fallback when failure occurs in HALF_OPEN state")
    void shouldExecuteFallbackOnFailureInHalfOpenState() {
      // Given: Circuit breaker in HALF_OPEN state with fallback
      var configuration = createCircuitBreakerConfigurationWithFallback();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> fallbackResult = Map.of("fallback", "executed");

      when(stateManager.getState(TASK_NAME)).thenReturn(CircuitBreakerState.State.HALF_OPEN);
      when(fallback.execute(any(TaskExecutionContext.class))).thenReturn(fallbackResult);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Service still failing");
          };

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should execute fallback, register failure, and transition to OPEN
      assertThat(result).isEqualTo(fallbackResult);
      verify(stateManager).incrementFailureCount(TASK_NAME);
      verify(fallback).execute(any(TaskExecutionContext.class));
      verify(stateManager).setState(TASK_NAME, CircuitBreakerState.State.OPEN);
      verify(stateManager).setOpenedTimestamp(eq(TASK_NAME), any(Instant.class));
      verify(stateManager).resetSuccessCount(TASK_NAME);
    }
  }

  @Nested
  @DisplayName("Fallback Scenarios")
  class FallbackScenarios {

    @Test
    @DisplayName("Should execute fallback when supplier fails and fallback is configured")
    void shouldExecuteFallbackOnFailure() {
      // Given: Circuit breaker with fallback configuration
      var configuration = createCircuitBreakerConfigurationWithFallback();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> fallbackResult = Map.of("fallback", "result");

      when(fallback.execute(any(TaskExecutionContext.class))).thenReturn(fallbackResult);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Primary service failed");
          };

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should return fallback result
      assertThat(result).isEqualTo(fallbackResult);
      verify(fallback).execute(any(TaskExecutionContext.class));
    }

    @Test
    @DisplayName("Should propagate fallback exception when fallback fails")
    void shouldPropagateFallbackExceptionWhenFallbackFails() {
      // Given: Circuit breaker with fallback that throws exception
      var configuration = createCircuitBreakerConfigurationWithFallback();
      var circuitBreaker = CircuitBreaker.of(configuration);
      var fallbackException = new WorkflowException("Fallback failed");

      when(fallback.execute(any(TaskExecutionContext.class))).thenThrow(fallbackException);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Primary service failed");
          };

      // When & Then: Should throw fallback exception
      assertThatThrownBy(() -> circuitBreaker.call(supplier)).isEqualTo(fallbackException);

      verify(fallback).execute(any(TaskExecutionContext.class));
    }

    @Test
    @DisplayName("Should execute fallback when success policy indicates failure")
    void shouldExecuteFallbackWhenSuccessPolicyIndicatesFailure() {
      // Given: Circuit breaker with both success policy and fallback
      var configuration = createCircuitBreakerConfigurationWithSuccessPolicyAndFallback();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> supplierResult = Map.of("data", "test");
      final Map<String, Serializable> policyResult = Map.of(RESULT, Boolean.FALSE);
      final Map<String, Serializable> fallbackResult = Map.of("fallback", "executed");

      when(taskExecutorService.executeTask(
              eq(TRANSACTION_ID), anyString(), any(ExecutionRequest.class)))
          .thenReturn(policyResult);
      when(fallback.execute(any(TaskExecutionContext.class))).thenReturn(fallbackResult);

      Supplier<Map<String, Serializable>> supplier = () -> supplierResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should execute fallback
      assertThat(result).isEqualTo(fallbackResult);
      verify(fallback).execute(any(TaskExecutionContext.class));
    }

    @Test
    @DisplayName("Should not re-register failure when fallback fails")
    void shouldNotReregisterFailureWhenFallbackFails() {
      // Given: Circuit breaker with fallback that throws exception
      var configuration = createCircuitBreakerConfigurationWithFallback();
      var circuitBreaker = CircuitBreaker.of(configuration);
      var fallbackException = new WorkflowException("Fallback failed");

      when(fallback.execute(any(TaskExecutionContext.class))).thenThrow(fallbackException);

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Primary service failed");
          };

      // When & Then: Should throw fallback exception and only register original failure
      assertThatThrownBy(() -> circuitBreaker.call(supplier)).isEqualTo(fallbackException);

      // Then: Should only register the original failure once, not the fallback failure
      verify(stateManager, times(1)).incrementFailureCount(TASK_NAME);
      verify(fallback).execute(any(TaskExecutionContext.class));
    }
  }

  @Nested
  @DisplayName("Configuration Scenarios")
  class ConfigurationScenarios {

    @Test
    @DisplayName("Should throw exception when configuration is null")
    void shouldThrowExceptionWhenConfigurationIsNull() {
      // When & Then: Should throw IllegalArgumentException
      assertThatThrownBy(() -> CircuitBreaker.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("configuration cannot be null");
    }

    @Test
    @DisplayName("Should handle different failure thresholds correctly")
    void shouldHandleDifferentFailureThresholds() {
      // Given: Circuit breaker with failure threshold of 1
      var configuration =
          new CircuitBreakerConfiguration(
              TASK_NAME,
              1L,
              BREAK_DURATION,
              2L,
              null,
              getTaskExecutionContext(),
              null,
              stateManager);

      var circuitBreaker = CircuitBreaker.of(configuration);

      when(stateManager.incrementFailureCount(TASK_NAME)).thenReturn(1L); // Meets threshold

      Supplier<Map<String, Serializable>> supplier =
          () -> {
            throw new WorkflowException("Single failure");
          };

      // When & Then: Should transition to OPEN after single failure
      assertThatThrownBy(() -> circuitBreaker.call(supplier))
          .isInstanceOf(WorkflowException.class)
          .hasMessage("Single failure");

      verify(stateManager).setState(TASK_NAME, CircuitBreakerState.State.OPEN);
    }

    @Test
    @DisplayName("Should handle different success thresholds correctly")
    void shouldHandleDifferentSuccessThresholds() {
      // Given: Circuit breaker in HALF_OPEN state with success threshold of 1
      when(stateManager.getState(TASK_NAME)).thenReturn(CircuitBreakerState.State.HALF_OPEN);

      var configuration =
          new CircuitBreakerConfiguration(
              TASK_NAME,
              3L,
              BREAK_DURATION,
              1L,
              null,
              getTaskExecutionContext(),
              null,
              stateManager);

      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> expectedResult = Map.of("status", "success");

      when(stateManager.incrementSuccessCount(TASK_NAME)).thenReturn(1L); // Meets threshold

      Supplier<Map<String, Serializable>> supplier = () -> expectedResult;

      // When: Calling the circuit breaker
      var result = circuitBreaker.call(supplier);

      // Then: Should transition to CLOSED after single success
      assertThat(result).isEqualTo(expectedResult);
      verify(stateManager).setState(TASK_NAME, CircuitBreakerState.State.CLOSED);
    }
  }

  @Nested
  @DisplayName("State Transition Edge Cases")
  class StateTransitionEdgeCases {

    @Test
    @DisplayName("Should handle multiple consecutive successful calls in CLOSED state")
    void shouldHandleMultipleConsecutiveSuccessfulCalls() {
      // Given: Circuit breaker in CLOSED state
      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> expectedResult = Map.of("status", "success");

      Supplier<Map<String, Serializable>> supplier = () -> expectedResult;

      // When: Making multiple successful calls
      for (int i = 0; i < 5; i++) {
        var result = circuitBreaker.call(supplier);
        assertThat(result).isEqualTo(expectedResult);
      }

      // Then: Should reset failure count on each success
      verify(stateManager, times(5)).resetFailureCount(TASK_NAME);
      verify(stateManager, never()).incrementFailureCount(TASK_NAME);
    }

    @Test
    @DisplayName("Should handle alternating success and failure in CLOSED state")
    void shouldHandleAlternatingSuccessAndFailure() {
      // Given: Circuit breaker in CLOSED state
      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);

      when(stateManager.incrementFailureCount(TASK_NAME))
          .thenReturn(1L, 1L); // Always 1, never reaches threshold

      var callCounter = new AtomicInteger(0);
      Supplier<Map<String, Serializable>> supplier =
          () -> {
            if (callCounter.incrementAndGet() % 2 == 0) {
              throw new WorkflowException("Every second call fails");
            }
            return Map.of("call", callCounter.get());
          };

      // When: Making alternating calls
      for (int i = 0; i < 4; i++) {
        if (i % 2 == 0) {
          // Success calls
          var result = circuitBreaker.call(supplier);
          assertThat(result).containsEntry("call", i + 1);
        } else {
          // Failure calls
          assertThatThrownBy(() -> circuitBreaker.call(supplier))
              .isInstanceOf(WorkflowException.class);
        }
      }

      // Then: Should not open circuit due to resets
      verify(stateManager, never()).setState(TASK_NAME, CircuitBreakerState.State.OPEN);
    }

    @Test
    @DisplayName("Should handle null opened timestamp gracefully")
    void shouldHandleNullOpenedTimestampGracefully() {
      // Given: Circuit breaker in OPEN state with null timestamp
      when(stateManager.getState(TASK_NAME)).thenReturn(CircuitBreakerState.State.OPEN);
      when(stateManager.getOpenedTimestamp(TASK_NAME)).thenReturn(null);

      var configuration = createBasicCircuitBreakerConfiguration();
      var circuitBreaker = CircuitBreaker.of(configuration);
      final Map<String, Serializable> expectedResult = Map.of("status", "success");

      Supplier<Map<String, Serializable>> supplier = () -> expectedResult;

      // When: Calling the circuit breaker (should treat as expired)
      var result = circuitBreaker.call(supplier);

      // Then: Should transition to HALF_OPEN and execute
      assertThat(result).isEqualTo(expectedResult);
      verify(stateManager).setState(TASK_NAME, CircuitBreakerState.State.HALF_OPEN);
    }
  }

  // Helper methods for creating configurations
  private CircuitBreakerConfiguration createBasicCircuitBreakerConfiguration() {
    return new CircuitBreakerConfiguration(
        TASK_NAME, 2L, BREAK_DURATION, 2L, null, getTaskExecutionContext(), null, stateManager);
  }

  private CircuitBreakerConfiguration createCircuitBreakerConfigurationWithSuccessPolicy() {
    when(successPolicy.task()).thenReturn("success-policy-task");
    when(successPolicy.metadata()).thenReturn(Map.of("__expression", "result.status == 'success'"));

    return new CircuitBreakerConfiguration(
        TASK_NAME,
        2L,
        BREAK_DURATION,
        2L,
        successPolicy,
        getTaskExecutionContext(),
        null,
        stateManager);
  }

  private CircuitBreakerConfiguration createCircuitBreakerConfigurationWithFallback() {
    when(fallbackTaskProcessor.task()).thenReturn("fallback-task");
    when(fallback.taskProcessor()).thenReturn(fallbackTaskProcessor);

    return new CircuitBreakerConfiguration(
        TASK_NAME, 2L, BREAK_DURATION, 2L, null, getTaskExecutionContext(), fallback, stateManager);
  }

  private CircuitBreakerConfiguration
      createCircuitBreakerConfigurationWithSuccessPolicyAndFallback() {
    when(successPolicy.task()).thenReturn("success-policy-task");
    when(successPolicy.metadata()).thenReturn(Map.of("__expression", "result.status == 'success'"));
    when(fallbackTaskProcessor.task()).thenReturn("fallback-task");
    when(fallback.taskProcessor()).thenReturn(fallbackTaskProcessor);

    return new CircuitBreakerConfiguration(
        TASK_NAME,
        2L,
        BREAK_DURATION,
        2L,
        successPolicy,
        getTaskExecutionContext(),
        fallback,
        stateManager);
  }

  private TaskExecutionContext getTaskExecutionContext() {
    return new TaskExecutionContext(TRANSACTION_ID, FLOW_ID, CORRELATION_ID, taskExecutorService);
  }
}
