package co.orquex.sagas.core.resilience.impl;

import co.orquex.sagas.core.resilience.RetryStateManager;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of RetryStateManager that stores retry state using thread-safe
 * concurrent data structures.
 *
 * <p>This implementation maintains retry attempt counters in memory using a ConcurrentHashMap with
 * AtomicLong values for thread-safe operations. All state is lost when the application terminates,
 * making this suitable for single-instance deployments or scenarios where retry state persistence
 * across restarts is not required.
 *
 * <p>Thread-safe and supports concurrent access from multiple threads.
 *
 * @see RetryStateManager
 */
public class InMemoryRetryStateManager implements RetryStateManager {

  private final Map<String, AtomicLong> registry = new ConcurrentHashMap<>();

  /**
   * Adds a new retry state entry for the specified identifier. Initializes the retry counter to
   * zero.
   *
   * @param name the retry state identifier to add
   */
  @Override
  public void add(String name) {
    if (name == null) {
      return; // Ignore null names silently
    }
    final var atomicLong = new AtomicLong(0L);
    registry.put(name, atomicLong);
  }

  /**
   * Gets the current retry count value for the specified name.
   *
   * @param name the retry state name
   * @return the current retry count value, or 0 if not found
   */
  @Override
  public long value(String name) {
    if (name == null) {
      return 0L;
    }
    return Optional.ofNullable(registry.get(name)).map(AtomicLong::longValue).orElse(0L);
  }

  /**
   * Increments the retry count atomically and returns the new value.
   *
   * @param name the retry state name
   * @return the new retry count after increment, or 0 if entry not found
   */
  @Override
  public long incrementAndGet(String name) {
    if (name == null) {
      return 0L;
    }
    return Optional.ofNullable(registry.get(name))
        .map(AtomicLong::incrementAndGet)
        .orElseGet(
            () -> {
              this.add(name);
              return this.incrementAndGet(name);
            });
  }

  /**
   * Resets the retry count to zero for the specified name.
   *
   * @param name the retry state name
   */
  @Override
  public void reset(String name) {
    Optional.ofNullable(registry.get(name)).ifPresent(atomicLong -> atomicLong.set(0L));
  }
}
