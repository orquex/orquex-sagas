package co.orquex.sagas.core.resilience;

import static co.orquex.sagas.domain.utils.Maps.merge;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNull;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;
import static org.slf4j.LoggerFactory.*;

import co.orquex.sagas.core.task.TaskExecutionContext;
import co.orquex.sagas.domain.task.TaskProcessor;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Fallback mechanism for circuit breaker patterns that provides alternative execution when primary
 * operations fail or the circuit is in an open state.
 *
 * <p>This record encapsulates the necessary components for fallback execution including metadata,
 * payload data, and the task processor that handles the fallback logic.
 *
 * @param metadata configuration and context information for fallback execution
 * @param payload data to be processed during fallback execution
 * @param taskProcessor processor that handles the actual fallback logic
 * @see TaskProcessor
 * @see TaskExecutionContext
 */
public record CircuitBreakerFallback(
    Map<String, Serializable> metadata,
    Map<String, Serializable> payload,
    TaskProcessor taskProcessor) {

  private static final Logger log = getLogger(CircuitBreakerFallback.class);

  /**
   * Constructs a CircuitBreakerFallback with validated parameters. Ensures metadata and payload are
   * never null by defaulting to empty maps, and validates that the task processor is provided.
   *
   * @throws IllegalArgumentException if taskProcessor is null
   */
  public CircuitBreakerFallback {
    metadata = checkArgumentNotNullOrElse(metadata, HashMap.newHashMap(0));
    payload = checkArgumentNotNullOrElse(payload, HashMap.newHashMap(0));
    checkArgumentNotNull(taskProcessor, "Task processor required");
  }

  /**
   * Executes the fallback logic using the provided execution context. Creates an execution request
   * with the fallback's metadata and payload, then delegates execution to the task executor
   * service.
   *
   * @param context the execution context containing transaction ID and executor service
   * @return map containing the results of the fallback execution
   */
  public Map<String, Serializable> execute(TaskExecutionContext context) {
    log.trace("Executing fallback for task {}", taskProcessor.task());
    final var transactionId = context.transactionId();
    final var executorService = context.taskExecutorService();
    final var taskMetadata = taskProcessor.metadata();
    final var executorRequest = context.createRequest(merge(metadata(), taskMetadata), payload);
    return executorService.executeTask(transactionId, taskProcessor.task(), executorRequest);
  }
}
