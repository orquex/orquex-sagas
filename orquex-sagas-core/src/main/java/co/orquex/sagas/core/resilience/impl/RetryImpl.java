package co.orquex.sagas.core.resilience.impl;

import static co.orquex.sagas.core.stage.strategy.impl.EvaluationProcessingStrategy.RESULT;

import co.orquex.sagas.core.resilience.Retry;
import co.orquex.sagas.core.resilience.RetryConfiguration;
import co.orquex.sagas.core.resilience.RetryState;
import co.orquex.sagas.core.resilience.exception.MaxRetriesExceededException;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.utils.Preconditions;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the Retry interface that provides automatic retry capabilities with
 * configurable policies for handling transient failures.
 *
 * <p>This implementation supports retry on exceptions, result validation, configurable wait
 * intervals, and maximum attempt limits. It maintains retry state and handles both workflow
 * exceptions and result-based retries.
 *
 * <p>Thread-safe implementation that can handle concurrent retry operations.
 *
 * @see Retry
 * @see RetryConfiguration
 * @see RetryState
 */
@Slf4j
public class RetryImpl implements Retry {

  private final AtomicReference<RuntimeException> lastRuntimeException = new AtomicReference<>();
  private final RetryState state;
  private final RetryConfiguration configuration;

  /**
   * Constructs a RetryImpl with the specified configuration. Initializes retry state and validates
   * the provided configuration.
   *
   * @param configuration the retry configuration defining behavior
   * @throws IllegalArgumentException if configuration is null
   */
  public RetryImpl(RetryConfiguration configuration) {
    Preconditions.checkArgumentNotNull(configuration, "Retry configuration cannot be null");

    this.state = new RetryState(configuration.task(), configuration.stateManager());
    this.configuration = configuration;

    log.debug(
        "Retry '{}' initialized with max attempts: {}, wait duration: {}ms, retry workflow exception: {}",
        configuration.task(),
        configuration.maxAttempts(),
        configuration.waitDuration().toMillis(),
        configuration.retryWorkflowException());
  }

  /**
   * Executes the provided supplier with retry logic according to the configuration. Continues
   * retrying until successful execution, result validation passes, or maximum attempts are
   * exceeded.
   *
   * @param supplier the operation to execute with retry capability
   * @return the result of the successful execution
   * @throws MaxRetriesExceededException if all retry attempts are exhausted
   * @throws WorkflowException if retry is not configured for workflow exceptions
   */
  @Override
  public Map<String, Serializable> call(Supplier<Map<String, Serializable>> supplier) {
    // Reset state at the beginning of each retry cycle
    this.reset();

    // Checks if the number of attempts is lower than the max attempts
    while (state.increment() <= configuration.maxAttempts()) {
      log.trace(
          "Retry '{}' attempt {}/{}",
          configuration.task(),
          state.value(),
          configuration.maxAttempts());
      try {
        final Map<String, Serializable> result = supplier.get();
        final var isResultValid = this.handleResult(result);
        if (isResultValid) {
          return result;
        }
      } catch (MaxRetriesExceededException e) {
        throw e;
      } catch (WorkflowException e) {
        this.handleError(e);
      }
    }
    // If we reach here, all retries are exhausted
    if (lastRuntimeException.get() != null) {
      throw lastRuntimeException.get();
    }
    throw new MaxRetriesExceededException(
        "Retry '%s' has exhausted all attempts (%d) for result validation"
            .formatted(configuration.task(), configuration.maxAttempts()));
  }

  /**
   * Handles result validation and determines if retry is needed based on the result. If result
   * validation is configured, executes the validation task processor.
   *
   * @param result the execution result to validate
   * @return true if result is valid and retry should stop, false if retry should continue
   */
  private boolean handleResult(Map<String, Serializable> result) {
    // Check if the configuration has retried on the result task processor
    final var successPolicyTask = configuration.successPolicyTask();

    boolean isRetryOnResult = null != successPolicyTask;
    if (!isRetryOnResult || isResultValid(result)) {
      return true;
    }

    final var waitDuration = configuration.waitDuration();
    waitInterval(waitDuration.toMillis());
    return false;
  }

  /**
   * Handles workflow exceptions according to the retry configuration. Determines whether to retry
   * or propagate the exception based on settings.
   *
   * @param workflowException the workflow exception that occurred
   * @throws WorkflowException if retry is not configured for workflow exceptions
   */
  private void handleError(WorkflowException workflowException) {
    final var maxAttempts = configuration.maxAttempts();
    final var waitDuration = configuration.waitDuration();
    final var currentNumberOfAttempts = state.value();
    final var retryOnWorkflowException = configuration.retryWorkflowException();

    if (retryOnWorkflowException
        && currentNumberOfAttempts > 0
        && currentNumberOfAttempts <= maxAttempts) {
      lastRuntimeException.set(workflowException);
      this.waitInterval(waitDuration.toMillis());
    } else {
      throw workflowException;
    }
  }

  /**
   * Waits for the specified interval before the next retry attempt. Handles thread interruption
   * gracefully.
   *
   * @param interval the wait interval in milliseconds
   * @throws WorkflowException if the thread is interrupted during wait
   */
  private void waitInterval(long interval) {
    try {
      log.trace("Retry '{}' waiting for {}ms", configuration.task(), interval);
      Thread.sleep(interval);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      final var toThrow = lastRuntimeException.get();
      if (toThrow != null) {
        throw toThrow;
      }
      throw new WorkflowException("Retry '%s' was interrupted".formatted(configuration.task()));
    }
  }

  /**
   * Validates the execution result using the configured result validation task processor. Executes
   * the validation task and checks if the result indicates success.
   *
   * @param payload the payload to validate
   * @return true if validation passes, false if validation fails or error occurs
   */
  private boolean isResultValid(Map<String, Serializable> payload) {
    // Execute the task processor and check if the payload is valid
    final var successPolicyTask = configuration.successPolicyTask();
    final var executionContext = configuration.taskExecutionContext();
    final var transactionId = executionContext.transactionId();
    final var executionRequest =
        executionContext.createRequest(successPolicyTask.metadata(), payload);
    final var executorService = executionContext.taskExecutorService();

    try {
      final var response =
          executorService.executeTask(transactionId, successPolicyTask.task(), executionRequest);
      return response.containsKey(RESULT)
          && response.get(RESULT) instanceof Boolean result
          && result;
    } catch (WorkflowException e) {
      log.error(
          "Error executing retry task processor '{}' for task '{}'",
          successPolicyTask.task(),
          configuration.task(),
          e);
      return false;
    }
  }

  /** Resets the retry state to allow for fresh retry cycles. */
  private void reset() {
    state.reset();
    lastRuntimeException.set(null);
    log.trace("Retry '{}' state has been reset", configuration.task());
  }
}
