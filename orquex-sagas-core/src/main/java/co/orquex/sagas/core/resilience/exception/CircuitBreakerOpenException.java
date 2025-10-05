package co.orquex.sagas.core.resilience.exception;

import co.orquex.sagas.core.resilience.CircuitBreakerFallback;
import co.orquex.sagas.core.resilience.CircuitBreakerState;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;

/**
 * Exception thrown when a circuit breaker is in OPEN state and rejects calls.
 *
 * <p>This exception indicates that the protected service is likely experiencing issues and the
 * circuit breaker is preventing further calls to allow recovery time. When this exception is
 * thrown, it means the circuit breaker has detected too many failures and has opened to protect the
 * downstream service.
 *
 * <p>This exception is thrown in the following scenarios:
 *
 * <ul>
 *   <li>When the circuit breaker is in OPEN state and no fallback is configured
 *   <li>When the circuit breaker actively rejects calls to protect the failing service
 * </ul>
 *
 * <p>The circuit breaker will remain open for a configured duration before transitioning to
 * HALF_OPEN state to test if the service has recovered.
 *
 * @see CircuitBreakerState
 * @see CircuitBreakerFallback
 * @see WorkflowException
 */
public class CircuitBreakerOpenException extends WorkflowException {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public CircuitBreakerOpenException(String message) {
    super(message);
  }
}
