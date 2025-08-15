package co.orquex.sagas.domain.task.resilience;

import co.orquex.sagas.domain.task.TaskProcessor;
import co.orquex.sagas.domain.utils.Preconditions;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

/**
 * Configuration record for task retry mechanism that defines retry behavior parameters.
 *
 * <p>This configuration encapsulates all necessary settings for task retry operations including
 * attempt limits, timing configuration, exception handling preferences, and conditional retry
 * logic. It ensures that retry operations are performed consistently and reliably across the saga
 * workflow.
 *
 * <p>The configuration validates all parameters to ensure they are within acceptable ranges and not
 * null where required, preventing runtime errors during task execution.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "maxAttempts": 3,
 *   "waitDuration": "PT5S",
 *   "retryWorkflowException": true,
 *   "successPolicyTask": {
 *     "task": "task-id",
 *     "metadata": {}
 *   }
 * }
 * </pre>
 *
 * @param maxAttempts maximum number of retry attempts allowed. Must be greater than 0.
 * @param waitDuration delay between retry attempts. Must not be null and must be positive.
 * @param retryWorkflowException whether to retry when workflow exceptions occur
 * @param successPolicyTask optional task processor for conditional retry based on task results. Can
 *     be null if conditional retry is not needed.
 * @since 1.0.0
 * @see TaskProcessor
 * @see Duration
 */
public record TaskRetryConfiguration(
    long maxAttempts,
    Duration waitDuration,
    boolean retryWorkflowException,
    TaskProcessor successPolicyTask)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  /**
   * Validates all required parameters using Preconditions.
   *
   * @throws IllegalArgumentException if any validation fails
   */
  public TaskRetryConfiguration {
    Preconditions.checkArgument(maxAttempts > 0, "Max attempts must be greater than 0");
    Preconditions.checkArgumentNotNull(waitDuration, "Wait duration cannot be null");
    Preconditions.checkArgument(
        !waitDuration.isNegative() && !waitDuration.isZero(), "Wait duration must be positive");
  }
}
