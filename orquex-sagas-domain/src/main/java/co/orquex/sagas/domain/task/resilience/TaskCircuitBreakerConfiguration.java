package co.orquex.sagas.domain.task.resilience;

import co.orquex.sagas.domain.task.TaskProcessor;
import co.orquex.sagas.domain.utils.Preconditions;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

/**
 * Configuration record for task circuit breaker mechanism that defines circuit breaker behavior
 * parameters.
 *
 * <p>This configuration encapsulates all necessary settings for circuit breaker operations
 * including failure thresholds, wait duration in open state, success requirements, and fallback
 * handling. It ensures that circuit breaker operations protect the system from cascading failures
 * and provide graceful degradation.
 *
 * <p>The configuration validates all parameters to ensure they are within acceptable ranges and not
 * null where required, preventing runtime errors during task execution.
 *
 * <p>JSON representation:
 *
 * <pre>
 *  {
 *    "failureThreshold": 5,
 *    "waitDurationInOpenState": "PT30S",
 *    "successThreshold": 2,
 *    "successPolicyTask": {
 *      "task": "task-id",
 *      "metadata": {}
 *    },
 *    "fallbackTask": {
 *      "task": "task-id",
 *      "metadata": {}
 *    }
 *  }
 *  </pre>
 *
 * @param failureThreshold maximum number of failures before opening the circuit. Must be greater
 *     than 0.
 * @param waitDurationInOpenState duration to keep the circuit open after failure threshold is reached. Must
 *     not be null and must be positive.
 * @param successThreshold number of successful calls required to close the circuit from half-open
 *     state. Must be greater than 0.
 * @param successPolicyTask optional task processor for handling successful operations. Can be null
 *     if not needed.
 * @param fallbackTask optional task processor for fallback operations when circuit is open. Can be
 *     null if not needed.
 * @since 1.0.0
 * @see TaskProcessor
 * @see Duration
 */
public record TaskCircuitBreakerConfiguration(
    long failureThreshold,
    Duration waitDurationInOpenState,
    long successThreshold,
    TaskProcessor successPolicyTask,
    TaskProcessor fallbackTask)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public TaskCircuitBreakerConfiguration {
    Preconditions.checkArgument(failureThreshold > 0, "Failure threshold must be greater than 0");
    Preconditions.checkArgumentNotNull(waitDurationInOpenState, "Wait duration in open state cannot be null");
    Preconditions.checkArgument(
        !waitDurationInOpenState.isNegative() && !waitDurationInOpenState.isZero(), "Wait duration in open state must be positive");
    Preconditions.checkArgument(successThreshold > 0, "Success threshold must be greater than 0");
  }
}
