package co.orquex.sagas.core.resilience.impl;

import static co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy.RESULT;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import co.orquex.sagas.core.resilience.CircuitBreaker;
import co.orquex.sagas.core.resilience.CircuitBreakerConfiguration;
import co.orquex.sagas.core.resilience.CircuitBreakerState;
import co.orquex.sagas.core.resilience.exception.CircuitBreakerOpenException;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskProcessor;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * Circuit Breaker resilience pattern implementation.
 *
 * <p>This implementation provides fault tolerance by monitoring failures and automatically
 * opening/closing the circuit to prevent cascading failures. It follows the classic three-state
 * circuit breaker pattern with configurable thresholds and timeouts.
 *
 * <h3>Circuit States:</h3>
 *
 * <ul>
 *   <li><strong>CLOSED:</strong> Normal operation - all calls are allowed through and monitored
 *   <li><strong>HALF_OPEN:</strong> Recovery testing - limited calls allowed to test service health
 *   <li><strong>OPEN:</strong> Failure protection - calls are rejected or redirected to fallback
 * </ul>
 *
 * <h3>State Transitions:</h3>
 *
 * <ul>
 *   <li><strong>CLOSED → OPEN:</strong> When failure threshold is exceeded
 *   <li><strong>OPEN → HALF_OPEN:</strong> When break duration expires
 *   <li><strong>HALF_OPEN → CLOSED:</strong> When success threshold is met
 *   <li><strong>HALF_OPEN → OPEN:</strong> When any failure occurs during testing
 * </ul>
 *
 * <h3>Key Features:</h3>
 *
 * <ul>
 *   <li>Configurable failure and success thresholds
 *   <li>Configurable break duration for recovery periods
 *   <li>Optional success policy validation for result evaluation
 *   <li>Optional fallback execution when circuit is open or failures occur
 *   <li>Thread-safe state management through pluggable state managers
 * </ul>
 *
 * @see CircuitBreaker
 * @see CircuitBreakerConfiguration
 */
public final class CircuitBreakerImpl implements CircuitBreaker {

  private static final Logger log = getLogger(CircuitBreakerImpl.class);

  /** Manages the persistent state of this circuit breaker instance */
  private final CircuitBreakerState state;

  /** Immutable configuration defining behavior and thresholds */
  private final CircuitBreakerConfiguration configuration;

  /**
   * Creates a new CircuitBreaker instance with the specified configuration.
   *
   * @param configuration the circuit breaker configuration containing thresholds, timeouts, and
   *     behavioral parameters
   * @throws IllegalArgumentException if configuration is null
   */
  public CircuitBreakerImpl(CircuitBreakerConfiguration configuration) {
    checkArgumentNotNull(configuration, "CircuitBreaker configuration cannot be null");

    this.state = new CircuitBreakerState(configuration.task(), configuration.stateManager());
    this.configuration = configuration;

    log.debug(
        "CircuitBreaker '{}' initialized with failure threshold: {}, break duration: {}ms, success threshold: {}",
        configuration.task(),
        configuration.failureThreshold(),
        configuration.waitDurationInOpenState().toMillis(),
        configuration.successThreshold());
  }

  /**
   * {@inheritDoc}
   *
   * <p>The execution flow varies based on the current circuit state:
   *
   * <ul>
   *   <li><strong>CLOSED:</strong> Executes supplier and monitors for failures
   *   <li><strong>HALF_OPEN:</strong> Executes supplier and transitions state based on result
   *   <li><strong>OPEN:</strong> Rejects calls or executes fallback if configured
   * </ul>
   *
   * @throws CircuitBreakerOpenException when the circuit is open and no fallback is configured
   * @throws WorkflowException when the supplier execution fails and no fallback is configured, or
   *     when both supplier and fallback fail
   * @throws IllegalArgumentException if supplier is null
   */
  @Override
  public Map<String, Serializable> call(Supplier<Map<String, Serializable>> supplier) {
    final CircuitBreakerState.State currentState = determineCurrentState();

    log.trace(
        "CircuitBreaker '{}' executing call in state: {}", configuration.task(), currentState);

    return switch (currentState) {
      case CLOSED -> executeInClosedState(supplier);
      case HALF_OPEN -> executeInHalfOpenState(supplier);
      case OPEN -> executeInOpenState();
    };
  }

  /**
   * Determines the effective current state by checking if an OPEN circuit should transition to
   * HALF_OPEN.
   *
   * <p>This method implements the automatic state transition logic:
   *
   * <ul>
   *   <li>If circuit is OPEN and break duration has expired → transition to HALF_OPEN
   *   <li>Otherwise → return the stored state unchanged
   * </ul>
   *
   * @return the effective current state of the circuit breaker
   */
  private CircuitBreakerState.State determineCurrentState() {
    final CircuitBreakerState.State storedState = state.getCurrentState();

    // Auto-transition from OPEN to HALF_OPEN when wait duration expires
    if (storedState == CircuitBreakerState.State.OPEN) {
      final boolean waitDurationExpired =
          state.isWaitDurationInOpenStateExpired(
              configuration.waitDurationInOpenState().toMillis());

      if (waitDurationExpired) {
        log.trace(
            "CircuitBreaker '{}' wait duration in OPEN state expired, transitioning to HALF_OPEN",
            configuration.task());
        switchToHalfOpen();
        return CircuitBreakerState.State.HALF_OPEN;
      }
    }

    return storedState;
  }

  /**
   * Executes the supplier when the circuit is in CLOSED state.
   *
   * <p>In CLOSED state, all calls are allowed through and monitored for failures. Success resets
   * the failure count, while failures are recorded and may trigger a transition to OPEN state if
   * the threshold is exceeded.
   *
   * @param supplier the function to execute
   * @return the result from supplier execution or fallback
   * @throws WorkflowException if supplier fails and no fallback is configured
   */
  private Map<String, Serializable> executeInClosedState(
      Supplier<Map<String, Serializable>> supplier) {

    Map<String, Serializable> result;

    // Execute the primary supplier and handle any exceptions
    try {
      result = supplier.get();
    } catch (WorkflowException e) {
      log.trace(
          "CircuitBreaker '{}' caught exception in CLOSED state: {}",
          configuration.task(),
          e.getMessage());
      return handleError(e);
    }

    // Evaluate result using success policy if configured
    if (hasExecutionError(result)) {
      log.trace(
          "CircuitBreaker '{}' detected execution error in CLOSED state", configuration.task());
      result = handleResult(result);
    } else {
      // Success path - reset failure counter
      registerSuccess();
    }

    return result;
  }

  /**
   * Executes the supplier when the circuit is in HALF_OPEN state.
   *
   * <p>In HALF_OPEN state, calls are allowed through to test service recovery. Any failure
   * immediately transitions back to OPEN state, while successes increment the success counter and
   * may close the circuit if threshold is met.
   *
   * @param supplier the function to execute
   * @return the result from supplier execution or fallback
   * @throws WorkflowException if supplier fails and no fallback is configured
   */
  private Map<String, Serializable> executeInHalfOpenState(
      Supplier<Map<String, Serializable>> supplier) {

    Map<String, Serializable> result;

    // Execute the primary supplier and handle any exceptions
    try {
      result = supplier.get();
    } catch (WorkflowException e) {
      log.trace(
          "CircuitBreaker '{}' caught exception in HALF_OPEN state, switching to OPEN",
          configuration.task());
      return handleError(e);
    }

    // Evaluate result using success policy if configured
    if (hasExecutionError(result)) {
      log.trace(
          "CircuitBreaker '{}' detected execution error in HALF_OPEN state, switching to OPEN",
          configuration.task());
      result = handleResult(result);
    } else {
      // Success in HALF_OPEN state - check if we can close the circuit
      handleSuccessInHalfOpenState();
    }

    return result;
  }

  /**
   * Handles successful execution in HALF_OPEN state.
   *
   * <p>Increments the success counter and transitions to CLOSED state if the success threshold is
   * met.
   */
  private void handleSuccessInHalfOpenState() {
    final long successCount = state.incrementSuccessCount();
    log.trace(
        "CircuitBreaker '{}' success #{} in HALF_OPEN state", configuration.task(), successCount);

    // Check if we've met the success threshold to close the circuit
    if (successCount >= configuration.successThreshold()) {
      log.trace(
          "CircuitBreaker '{}' success threshold met, switching to CLOSED", configuration.task());
      switchToClose();
    }
  }

  /**
   * Handles execution when the circuit is in OPEN state.
   *
   * <p>In OPEN state, calls are rejected to allow the failing service time to recover. If a
   * fallback is configured, it will be executed instead of rejecting the call.
   *
   * @return the result from fallback execution
   * @throws CircuitBreakerOpenException if no fallback is configured
   */
  private Map<String, Serializable> executeInOpenState() {
    final var fallback = configuration.fallback();

    if (fallback == null) {
      throw new CircuitBreakerOpenException(
          "CircuitBreaker '%s' is OPEN - calls are being rejected to allow service recovery"
              .formatted(configuration.task()));
    }

    return executeFallback();
  }

  /**
   * Handles failure detected through success policy validation.
   *
   * <p>This method is called when the supplier execution succeeds but the result is deemed a
   * failure by the success policy. It registers the failure and executes fallback if configured.
   *
   * @param result the original result from supplier execution
   * @return the original result or fallback result
   */
  private Map<String, Serializable> handleResult(Map<String, Serializable> result) {
    // Register failure first - this may trigger state transitions
    registerFailure();

    final var fallback = configuration.fallback();
    if (fallback == null) {
      return result;
    }

    return executeFallback();
  }

  /**
   * Handles exceptions thrown during supplier execution.
   *
   * <p>This method is called when the supplier execution throws a WorkflowException. It registers
   * the failure and executes fallback if configured, otherwise re-throws the original exception.
   *
   * @param originalException the exception thrown by the supplier
   * @return the result from fallback execution
   * @throws WorkflowException the original exception if no fallback is configured
   */
  private Map<String, Serializable> handleError(WorkflowException originalException) {
    // Register failure first - this may trigger state transitions
    registerFailure();

    final var fallback = configuration.fallback();
    if (fallback == null) {
      throw originalException;
    }

    return executeFallback();
  }

  /**
   * Executes the configured fallback when the primary operation fails.
   *
   * <p>Fallback execution failures are propagated to the caller without additional failure
   * registration, as the original failure has already been recorded.
   *
   * @return the result from fallback execution
   */
  private Map<String, Serializable> executeFallback() {
    final var fallback = configuration.fallback();
    final String fallbackTaskName = fallback.taskProcessor().task();

    try {
      log.trace(
          "CircuitBreaker '{}' executing fallback task '{}'",
          configuration.task(),
          fallbackTaskName);

      final Map<String, Serializable> fallbackResult =
          fallback.execute(configuration.taskExecutionContext());

      log.trace(
          "CircuitBreaker '{}' fallback task '{}' executed successfully",
          configuration.task(),
          fallbackTaskName);

      return fallbackResult;

    } catch (WorkflowException fallbackException) {
      log.error(
          "CircuitBreaker '{}' fallback task '{}' execution failed",
          configuration.task(),
          fallbackTaskName);

      // Propagate fallback failure without additional registration
      throw fallbackException;
    }
  }

  /**
   * Registers a successful execution and resets failure counter if in CLOSED state.
   *
   * <p>Success registration only occurs in CLOSED state to reset the failure count. In HALF_OPEN
   * state, success is handled separately to manage the success counter.
   */
  private void registerSuccess() {
    // Only reset failure count in CLOSED state
    if (state.getCurrentState() == CircuitBreakerState.State.CLOSED) {
      state.resetFailureCount();
      log.trace("CircuitBreaker '{}' success - failure count reset", configuration.task());
    }
  }

  /**
   * Registers a failed execution and evaluates state transition conditions.
   *
   * <p>This method implements the core failure tracking logic:
   *
   * <ul>
   *   <li>Increments the failure counter
   *   <li>Transitions to OPEN if failure threshold is exceeded in CLOSED state
   *   <li>Transitions to OPEN immediately if in HALF_OPEN state (any failure)
   * </ul>
   */
  private void registerFailure() {
    final long failureCount = state.incrementFailureCount();
    log.trace("CircuitBreaker '{}' failure #{}", configuration.task(), failureCount);

    final CircuitBreakerState.State currentState = state.getCurrentState();
    final boolean shouldOpen =
        failureCount >= configuration.failureThreshold()
            || currentState == CircuitBreakerState.State.HALF_OPEN;

    if (shouldOpen) {
      log.warn(
          "CircuitBreaker '{}' threshold exceeded or failure in HALF_OPEN, switching to OPEN",
          configuration.task());
      switchToOpen();
    }
  }

  /**
   * Evaluates whether the execution result represents a failure using the success policy.
   *
   * <p>If no success policy is configured, all executions are considered successful (no
   * result-based failures). The success policy is a pluggable component that allows custom business
   * logic to determine success/failure based on the result.
   *
   * @param result the execution result to evaluate
   * @return {@code true} if the result represents a failure, {@code false} if successful
   */
  private boolean hasExecutionError(Map<String, Serializable> result) {
    final TaskProcessor successPolicy = configuration.successPolicy();

    // No success policy configured - consider all results successful
    if (successPolicy == null) {
      return false;
    }

    try {
      log.trace(
          "CircuitBreaker '{}' validating result using success policy '{}'",
          configuration.task(),
          successPolicy.task());

      final Map<String, Serializable> policyResponse = executeTaskProcessor(result, successPolicy);

      // Success policy should return a boolean result indicating success (true) or failure (false)
      final boolean isSuccess = extractBooleanResult(policyResponse);

      log.trace(
          "CircuitBreaker '{}' success policy '{}' result: {}",
          configuration.task(),
          successPolicy.task(),
          isSuccess);

      // Return true if it represents an error/failure
      return !isSuccess;

    } catch (WorkflowException e) {
      log.error(
          "CircuitBreaker '{}' error executing success policy '{}', treating as failure",
          configuration.task(),
          successPolicy.task(),
          e);
      // If policy execution fails, treat as error to be safe
      return true;
    }
  }

  /**
   * Extracts a boolean result from the success policy response.
   *
   * @param policyResponse the response from success policy execution
   * @return {@code true} if the policy indicates success, {@code false} otherwise
   */
  private boolean extractBooleanResult(Map<String, Serializable> policyResponse) {
    return policyResponse.containsKey(RESULT)
        && policyResponse.get(RESULT) instanceof Boolean policyResult
        && policyResult;
  }

  /**
   * Executes a task processor with the given result as input.
   *
   * @param result the execution result to pass to the task processor
   * @param taskProcessor the task processor to execute
   * @return the result from task processor execution
   * @throws WorkflowException if task processor execution fails
   */
  private Map<String, Serializable> executeTaskProcessor(
      Map<String, Serializable> result, TaskProcessor taskProcessor) {
    final var executionContext = configuration.taskExecutionContext();
    final var transactionId = executionContext.transactionId();
    final var executionRequest = executionContext.createRequest(taskProcessor.metadata(), result);
    final var executorService = executionContext.taskExecutorService();

    return executorService.executeTask(transactionId, taskProcessor.task(), executionRequest);
  }

  /**
   * Transitions the circuit to CLOSED state.
   *
   * <p>This transition occurs when the success threshold is met in HALF_OPEN state. All counters
   * are reset to prepare for normal operation.
   */
  private void switchToClose() {
    state.setState(CircuitBreakerState.State.CLOSED);
    state.resetFailureCount();
    state.resetSuccessCount();
    log.trace("CircuitBreaker '{}' switched to CLOSED state", configuration.task());
  }

  /**
   * Transitions the circuit to HALF_OPEN state.
   *
   * <p>This transition occurs when the break duration expires in OPEN state. The success counter is
   * reset to begin tracking recovery attempts.
   */
  private void switchToHalfOpen() {
    state.setState(CircuitBreakerState.State.HALF_OPEN);
    state.resetSuccessCount();
    log.trace("CircuitBreaker '{}' switched to HALF_OPEN state", configuration.task());
  }

  /**
   * Transitions the circuit to OPEN state.
   *
   * <p>This transition occurs when:
   *
   * <ul>
   *   <li>Failure threshold is exceeded in CLOSED state
   *   <li>Any failure occurs in HALF_OPEN state
   * </ul>
   *
   * <p>The opened timestamp is recorded for break duration calculation, and the success counter is
   * reset.
   */
  private void switchToOpen() {
    state.setState(CircuitBreakerState.State.OPEN);
    state.setOpenedTimestamp(Instant.now());
    state.resetSuccessCount();

    log.warn(
        "CircuitBreaker '{}' switched to OPEN state - failure count: {}",
        configuration.task(),
        state.getFailureCount());
  }
}
