package co.orquex.sagas.domain.task.resilience;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

/**
 * This class represents the configuration for resilience in a task. It includes timeout, retry and
 * circuit breaker configurations.
 *
 * <p>JSON Representation:
 *
 * <pre>
 * {
 *   "timeout": "PT1M",
 *   "retry": {},
 *   "circuitBreaker": {}
 * }
 * </pre>
 *
 * @see TaskRetryConfiguration
 * @see TaskCircuitBreakerConfiguration
 */
public record TaskResilienceConfiguration(
        Duration timeout, TaskRetryConfiguration retry, TaskCircuitBreakerConfiguration circuitBreaker)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);

  public TaskResilienceConfiguration {
    timeout = checkArgumentNotNullOrElse(timeout, DEFAULT_TIMEOUT);
  }
}
