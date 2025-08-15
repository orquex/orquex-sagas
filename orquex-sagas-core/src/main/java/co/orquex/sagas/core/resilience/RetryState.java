package co.orquex.sagas.core.resilience;

/**
 * Represents the state of a retry mechanism, providing a convenient wrapper around a
 * RetryStateManager to manage retry attempts and counters.
 *
 * <p>Encapsulates retry state operations for a named retry instance, allowing easy access to
 * current attempt counts and increment operations.
 *
 * @param name the unique name identifying this retry state
 * @param stateManager the state manager responsible for persisting retry state
 * @see RetryStateManager
 */
public record RetryState(String name, RetryStateManager stateManager) {

  /**
   * Gets the current retry attempt count.
   *
   * @return the current number of retry attempts
   */
  public long value() {
    return stateManager.value(name);
  }

  /**
   * Increments the retry attempt count and returns the new value. Should be called each time a
   * retry is attempted.
   *
   * @return the new retry count after increment
   */
  public long increment() {
    return stateManager.incrementAndGet(name);
  }

  /** Resets the retry attempt count to zero. Typically called when transitioning between states. */
  public void reset() {
    stateManager.reset(name);
  }
}
