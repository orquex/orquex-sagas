package co.orquex.sagas.core.resilience;

import co.orquex.sagas.core.resilience.impl.RetryImpl;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Interface for retry mechanism that provides automatic retry capabilities for operations that may
 * fail transiently.
 *
 * <p>Supports configurable retry policies including maximum attempts, delay strategies, and failure
 * conditions.
 *
 * @see RetryConfiguration
 * @see RetryImpl
 */
public interface Retry {

  /**
   * Creates a new Retry instance with the specified configuration.
   *
   * @param configuration the retry configuration defining behavior
   * @return a new Retry instance
   */
  static Retry of(RetryConfiguration configuration) {
    return new RetryImpl(configuration);
  }

  /**
   * Executes the provided supplier with retry logic.
   *
   * @param supplier the operation to execute with retry capability
   * @return the result of the successful execution
   */
  default Map<String, Serializable> call(Supplier<Map<String, Serializable>> supplier) {
    return supplier.get();
  }
}
