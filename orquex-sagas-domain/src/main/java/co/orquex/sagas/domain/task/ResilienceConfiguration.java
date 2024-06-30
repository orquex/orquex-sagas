package co.orquex.sagas.domain.task;

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
 * @see RetryConfiguration
 * @see CircuitBreakerConfiguration
 */
public record ResilienceConfiguration(
    Duration timeout, RetryConfiguration retry, CircuitBreakerConfiguration circuitBreaker)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);

  public ResilienceConfiguration {
    timeout = checkArgumentNotNullOrElse(timeout, DEFAULT_TIMEOUT);
  }
}
