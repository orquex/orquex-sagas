package co.orquex.sagas.core.resilience.exception;

import co.orquex.sagas.core.resilience.Retry;
import co.orquex.sagas.core.resilience.RetryConfiguration;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;

/**
 * Exception thrown when a retry operation has exhausted all configured attempts without achieving a
 * successful result.
 *
 * <p>This exception indicates that the retry mechanism has reached its maximum attempt limit and
 * cannot continue retrying the operation. It extends WorkflowException to integrate with the saga
 * framework's exception handling.
 *
 * @see Retry
 * @see RetryConfiguration
 * @see WorkflowException
 */
public class MaxRetriesExceededException extends WorkflowException {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public MaxRetriesExceededException(String message) {
    super(message);
  }
}
