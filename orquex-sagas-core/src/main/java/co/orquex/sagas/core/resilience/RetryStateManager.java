package co.orquex.sagas.core.resilience;

/**
 * Interface for managing retry state persistence and operations. Provides methods to track retry
 * attempts and manage retry counters across different storage implementations.
 *
 * <p>Implementations can store state in memory, database, cache, or other storage mechanisms
 * depending on persistence requirements.
 */
public interface RetryStateManager {

  /**
   * Adds a new retry state entry for the specified identifier.
   *
   * @param state the state identifier to add
   */
  void add(String state);

  /**
   * Gets the current retry count value for the specified name.
   *
   * @param name the retry state name
   * @return the current retry count value
   */
  long value(String name);

  /**
   * Increments the retry count atomically and returns the new value.
   *
   * @param name the retry state name
   * @return the new retry count after increment
   */
  long incrementAndGet(String name);

  /**
   * Resets the retry count to zero.
   *
   * @param name the retry state name
   */
  void reset(String name);
}
