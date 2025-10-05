package co.orquex.sagas.core.resilience;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNull;

import java.time.Instant;

/**
 * Represents the state and operations of a circuit breaker, providing a convenient wrapper around a
 * CircuitBreakerStateManager to manage circuit breaker behavior.
 *
 * <p>Encapsulates the circuit breaker's current state, failure/success counts, and timing
 * information for state transitions in the circuit breaker pattern.
 *
 * @see CircuitBreakerStateManager
 */
public record CircuitBreakerState(String name, CircuitBreakerStateManager stateManager) {

  public CircuitBreakerState {
    checkArgumentNotEmpty(name, "Circuit breaker name cannot be null or empty");
    checkArgumentNotNull(stateManager, "Circuit breaker state manager cannot be null");
  }

  /**
   * Enumeration of possible circuit breaker states following the standard circuit breaker pattern.
   */
  public enum State {
    /** Circuit is closed, allowing all calls through */
    CLOSED,
    /** Circuit is half-open, allowing limited calls to test service health */
    HALF_OPEN,
    /** Circuit is open, rejecting all calls */
    OPEN
  }

  /**
   * Gets the current state of the circuit breaker.
   *
   * @return the current state (CLOSED, HALF_OPEN, or OPEN)
   */
  public State getCurrentState() {
    final var currentStage = stateManager.getState(name);
    if (currentStage == null) {
      return State.CLOSED;
    }
    return currentStage;
  }

  /**
   * Sets the current state of the circuit breaker.
   *
   * @param state the new state to set
   */
  public void setState(State state) {
    stateManager.setState(name, state);
  }

  /**
   * Gets the current failure count for this circuit breaker. Used to determine when to transition
   * from CLOSED to OPEN state.
   *
   * @return the current failure count
   */
  public long getFailureCount() {
    return stateManager.getFailureCount(name);
  }

  /**
   * Gets the current success count for this circuit breaker. Primarily used in HALF_OPEN state to
   * determine transition back to CLOSED state.
   *
   * @return the current success count
   */
  public long getSuccessCount() {
    return stateManager.getSuccessCount(name);
  }

  /**
   * Increments the failure count and returns the new value. Should be called when a service call
   * fails.
   *
   * @return the new failure count after increment
   */
  public long incrementFailureCount() {
    return stateManager.incrementFailureCount(name);
  }

  /**
   * Increments the success count and returns the new value. Should be called when a service call
   * succeeds.
   *
   * @return the new success count after increment
   */
  public long incrementSuccessCount() {
    return stateManager.incrementSuccessCount(name);
  }

  /**
   * Resets the failure count to zero. Typically called when transitioning from OPEN to HALF_OPEN
   * state.
   */
  public void resetFailureCount() {
    stateManager.resetFailureCount(name);
  }

  /** Resets the success count to zero. Typically called when transitioning between states. */
  public void resetSuccessCount() {
    stateManager.resetSuccessCount(name);
  }

  /**
   * Gets the timestamp when this circuit was opened.
   *
   * @return the instant when the circuit was opened, or null if never opened
   */
  public Instant getOpenedTimestamp() {
    return stateManager.getOpenedTimestamp(name);
  }

  /**
   * Sets the timestamp when this circuit was opened. Used to calculate when the circuit should
   * transition from OPEN to HALF_OPEN state.
   *
   * @param timestamp the instant when the circuit was opened
   */
  public void setOpenedTimestamp(Instant timestamp) {
    stateManager.setOpenedTimestamp(name, timestamp);
  }

  /**
   * Checks if the wait duration in open state has expired for an open circuit. Determines whether
   * enough time has passed to allow transition to HALF_OPEN state.
   *
   * @param waitDurationInMillis the configured wait in open state duration in milliseconds
   * @return true if the wait duration in open state has expired, false otherwise
   */
  public boolean isWaitDurationInOpenStateExpired(long waitDurationInMillis) {
    final var openedTime = getOpenedTimestamp();
    if (openedTime == null) {
      return true;
    }
    return Instant.now().isAfter(openedTime.plusMillis(waitDurationInMillis));
  }
}
