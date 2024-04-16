package co.orquex.sagas.domain.task;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

public record ResilienceConfiguration(
    Duration timeout, RetryConfiguration retry, CircuitBreakerConfiguration circuitBreaker)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);

  public ResilienceConfiguration {
    timeout = checkArgumentNotNullOrElse(timeout, DEFAULT_TIMEOUT);
  }
}
