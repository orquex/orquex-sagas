package co.orquex.sagas.core.resilience.impl;

import co.orquex.sagas.core.resilience.CircuitBreakerState.State;
import co.orquex.sagas.core.resilience.CircuitBreakerStateManager;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of CircuitBreakerStateManager that stores circuit breaker state using
 * thread-safe concurrent data structures.
 *
 * <p>This implementation is suitable for single-instance deployments or scenarios where state
 * persistence across application restarts is not required. All state is lost when the application
 * terminates.
 *
 * <p>Thread-safe and supports concurrent access from multiple threads.
 *
 * @see CircuitBreakerStateManager
 */
public class InMemoryCircuitBreakerStateManager implements CircuitBreakerStateManager {

  private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> failureCounts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> successCounts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Instant> openedTimestamps = new ConcurrentHashMap<>();

  /**
   * Gets the current state of the circuit breaker.
   *
   * @param name the name of the circuit breaker
   * @return the current state, defaults to CLOSED if circuit breaker not found
   */
  @Override
  public State getState(String name) {
    if (name == null) {
      return State.CLOSED;
    }
    return states.getOrDefault(name, State.CLOSED);
  }

  /**
   * Sets the current state of the circuit breaker.
   *
   * @param name the name of the circuit breaker
   * @param state the new state to set
   */
  @Override
  public void setState(String name, State state) {
    if (name == null) {
      return; // Ignore null names silently
    }
    states.put(name, state);
  }

  /**
   * Gets the current failure count for the circuit breaker.
   *
   * @param name the name of the circuit breaker
   * @return the failure count, defaults to 0 if circuit breaker not found
   */
  @Override
  public long getFailureCount(String name) {
    if (name == null) {
      return 0L;
    }
    return failureCounts.getOrDefault(name, new AtomicLong(0)).get();
  }

  /**
   * Gets the current success count for the circuit breaker.
   *
   * @param name the name of the circuit breaker
   * @return the success count, defaults to 0 if circuit breaker not found
   */
  @Override
  public long getSuccessCount(String name) {
    if (name == null) {
      return 0L;
    }
    return successCounts.getOrDefault(name, new AtomicLong(0)).get();
  }

  /**
   * Increments the failure count for the circuit breaker atomically.
   *
   * @param name the name of the circuit breaker
   * @return the new failure count after increment
   */
  @Override
  public long incrementFailureCount(String name) {
    if (name == null) {
      return 0L;
    }
    return failureCounts.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
  }

  /**
   * Increments the success count for the circuit breaker atomically.
   *
   * @param name the name of the circuit breaker
   * @return the new success count after increment
   */
  @Override
  public long incrementSuccessCount(String name) {
    if (name == null) {
      return 0L;
    }
    return successCounts.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
  }

  /**
   * Resets the failure count to zero for the circuit breaker.
   *
   * @param name the name of the circuit breaker
   */
  @Override
  public void resetFailureCount(String name) {
    if (name == null) {
      return; // Ignore null names silently
    }
    failureCounts.put(name, new AtomicLong(0));
  }

  /**
   * Resets the success count to zero for the circuit breaker.
   *
   * @param name the name of the circuit breaker
   */
  @Override
  public void resetSuccessCount(String name) {
    if (name == null) {
      return; // Ignore null names silently
    }
    successCounts.put(name, new AtomicLong(0));
  }

  /**
   * Gets the timestamp when the circuit breaker was opened.
   *
   * @param name the name of the circuit breaker
   * @return the timestamp when opened, or null if never opened or not found
   */
  @Override
  public Instant getOpenedTimestamp(String name) {
    if (name == null) {
      return null;
    }
    return openedTimestamps.get(name);
  }

  /**
   * Sets the timestamp when the circuit breaker was opened.
   *
   * @param name the name of the circuit breaker
   * @param timestamp the timestamp when the circuit was opened
   */
  @Override
  public void setOpenedTimestamp(String name, Instant timestamp) {
    if (name == null) {
      return; // Ignore null names silently
    }
    openedTimestamps.put(name, timestamp);
  }
}
