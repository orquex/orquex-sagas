package co.orquex.sagas.core.resilience;

import co.orquex.sagas.core.task.TaskExecutionContext;
import co.orquex.sagas.domain.task.TaskProcessor;
import co.orquex.sagas.domain.utils.Preconditions;
import java.time.Duration;

/**
 * Immutable configuration for CircuitBreaker resilience pattern.
 *
 * <p>This configuration defines all behavioral parameters and thresholds that control how a circuit
 * breaker operates. It serves as the blueprint for circuit breaker initialization and behavior
 * throughout its lifecycle.
 *
 * <h3>Configuration Parameters:</h3>
 *
 * <ul>
 *   <li><strong>Basic Identity:</strong> Task name for identification and logging
 *   <li><strong>Failure Management:</strong> Threshold for opening the circuit
 *   <li><strong>Recovery Control:</strong> Break duration and success threshold
 *   <li><strong>Advanced Features:</strong> Success policy, fallback, and state management
 * </ul>
 *
 * <h3>State Transition Parameters:</h3>
 *
 * <ul>
 *   <li><strong>failureThreshold:</strong> Controls CLOSED → OPEN transition
 *   <li><strong>waitDurationInOpenState:</strong> Controls OPEN → HALF_OPEN transition timing
 *   <li><strong>successThreshold:</strong> Controls HALF_OPEN → CLOSED transition
 * </ul>
 *
 * @param task The unique name/identifier of the task this circuit breaker protects. Used for
 *     logging, monitoring, and state management. Must not be null or empty.
 * @param failureThreshold The number of consecutive failures that will trigger the circuit to
 *     transition from CLOSED to OPEN state. Must be positive.
 * @param waitDurationInOpenState The duration the circuit should remain in OPEN state before
 *     automatically transitioning to HALF_OPEN for recovery testing. Must be positive.
 * @param successThreshold The number of consecutive successes required in HALF_OPEN state to
 *     transition back to CLOSED state. Must be positive.
 * @param successPolicy Optional task processor that validates if a result represents success. If
 *     null, only exceptions are considered failures. When provided, this policy receives the
 *     execution result and should return a boolean indicating success (true) or failure (false).
 * @param taskExecutionContext The execution context required for running tasks, including
 *     transaction information and executor services. Must not be null.
 * @param fallback Optional fallback task processor executed when the circuit is open or when
 *     primary execution fails. If null, failures will result in exceptions. When provided, offers
 *     graceful degradation capabilities.
 * @param stateManager The state manager responsible for persisting circuit breaker state across
 *     calls and potentially across application restarts. Must not be null. Choose implementation
 *     based on your persistence requirements.
 * @see CircuitBreakerState
 * @see CircuitBreakerStateManager
 * @see CircuitBreakerFallback
 * @see TaskProcessor
 * @see TaskExecutionContext
 */
public record CircuitBreakerConfiguration(
    String task,
    long failureThreshold,
    Duration waitDurationInOpenState,
    long successThreshold,
    TaskProcessor successPolicy,
    TaskExecutionContext taskExecutionContext,
    CircuitBreakerFallback fallback,
    CircuitBreakerStateManager stateManager) {

  /**
   * Validates all required parameters using Preconditions.
   *
   * @throws IllegalArgumentException if any validation fails
   */
  public CircuitBreakerConfiguration {
    Preconditions.checkArgumentNotEmpty(task, "Task name cannot be null or empty");
    Preconditions.checkArgument(failureThreshold > 0, "Failure threshold must be positive");
    Preconditions.checkArgumentNotNull(
        waitDurationInOpenState, "Wait duration in open state cannot be null");
    Preconditions.checkArgument(
        !waitDurationInOpenState.isNegative() && !waitDurationInOpenState.isZero(),
        "Wait duration in open state must be positive");
    Preconditions.checkArgument(successThreshold > 0, "Success threshold must be positive");
    Preconditions.checkArgumentNotNull(
        taskExecutionContext, "Task execution context cannot be null");
    Preconditions.checkArgumentNotNull(stateManager, "State manager cannot be null");
  }
}
