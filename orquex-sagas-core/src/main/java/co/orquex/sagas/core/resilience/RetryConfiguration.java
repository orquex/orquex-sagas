package co.orquex.sagas.core.resilience;

import co.orquex.sagas.core.task.TaskExecutionContext;
import co.orquex.sagas.domain.task.TaskProcessor;
import java.time.Duration;

/**
 * Configuration record for retry mechanism that defines retry behavior parameters.
 *
 * <p>Encapsulates all necessary settings for retry operations including attempt limits, timing
 * configuration, exception handling, and state management.
 *
 * @param task the name of the task to be retried
 * @param maxAttempts maximum number of retry attempts allowed
 * @param waitDuration delay between retry attempts
 * @param retryWorkflowException whether to retry on workflow exceptions
 * @param successPolicyTask task processor for conditional retry based on results
 * @param stateManager manager for persisting retry state
 * @param taskExecutionContext execution context for retry operations
 * @see TaskProcessor
 * @see RetryStateManager
 * @see TaskExecutionContext
 */
public record RetryConfiguration(
    String task,
    long maxAttempts,
    Duration waitDuration,
    boolean retryWorkflowException,
    TaskProcessor successPolicyTask,
    RetryStateManager stateManager,
    TaskExecutionContext taskExecutionContext) {}

