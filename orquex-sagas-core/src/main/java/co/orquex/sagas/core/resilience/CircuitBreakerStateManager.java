package co.orquex.sagas.core.resilience;

import java.time.Instant;

/**
 * Interface for managing circuit breaker state persistence and operations. Implementations can
 * store state in memory, database, cache, or other storage mechanisms.
 *
 * <p>This interface provides the contract for managing circuit breaker state including current
 * state, failure/success counts, and timing information across different storage backends.
 */
public interface CircuitBreakerStateManager {

  /**
   * Gets the current state of the circuit breaker.
   *
   * @param name circuit breaker name
   * @return current state, defaults to CLOSED if not found
   */
  CircuitBreakerState.State getState(String name);

  /**
   * Sets the current state of the circuit breaker.
   *
   * @param name circuit breaker name
   * @param state new state to set
   */
  void setState(String name, CircuitBreakerState.State state);

  /**
   * Gets the failure count for the circuit breaker.
   *
   * @param name circuit breaker name
   * @return failure count, defaults to 0 if not found
   */
  long getFailureCount(String name);

  /**
   * Gets the success count for the circuit breaker. Used in half-open state to track successful
   * calls.
   *
   * @param name circuit breaker name
   * @return success count, defaults to 0 if not found
   */
  long getSuccessCount(String name);

  /**
   * Increments and returns the failure count.
   *
   * @param name circuit breaker name
   * @return new failure count after increment
   */
  long incrementFailureCount(String name);

  /**
   * Increments and returns the success count.
   *
   * @param name circuit breaker name
   * @return new success count after increment
   */
  long incrementSuccessCount(String name);

  /**
   * Resets the failure count to zero.
   *
   * @param name circuit breaker name
   */
  void resetFailureCount(String name);

  /**
   * Resets the success count to zero.
   *
   * @param name circuit breaker name
   */
  void resetSuccessCount(String name);

  /**
   * Gets the timestamp when the circuit was opened.
   *
   * @param name circuit breaker name
   * @return opened timestamp, null if never opened or not found
   */
  Instant getOpenedTimestamp(String name);

  /**
   * Sets the timestamp when the circuit was opened.
   *
   * @param name circuit breaker name
   * @param timestamp timestamp when opened
   */
  void setOpenedTimestamp(String name, Instant timestamp);
}
