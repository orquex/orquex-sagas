package co.orquex.sagas.core.task;

import co.orquex.sagas.domain.execution.ExecutionRequest;
import java.io.Serializable;
import java.util.Map;

/**
 * Immutable context container that encapsulates the execution environment for tasks within a saga
 * workflow.
 *
 * <p>This record provides essential contextual information needed for task execution, including
 * transaction identifiers, workflow references, and access to the task execution service. It serves
 * as a bridge between the workflow orchestration layer and individual task executions.
 *
 * <p>The context is designed to be passed through the workflow execution pipeline, ensuring that
 * each task has access to:
 *
 * <ul>
 *   <li>Unique transaction and correlation identifiers for tracing and debugging
 *   <li>Flow identification for workflow management
 *   <li>Task execution service for performing actual task operations
 * </ul>
 *
 * <p>As a record, this class is immutable by design, ensuring thread safety and preventing
 * accidental modification during concurrent task execution.
 *
 * @param transactionId the unique identifier for the overall transaction or saga instance
 * @param flowId the identifier of the specific workflow or flow being executed
 * @param correlationId the correlation identifier used for tracking related operations across
 *     services
 * @param taskExecutorService the service responsible for executing individual tasks within the
 *     workflow
 * @since 1.0.0
 * @see ExecutionRequest
 * @see TaskExecutorService
 */
public record TaskExecutionContext(
    String transactionId,
    String flowId,
    String correlationId,
    TaskExecutorService taskExecutorService) {

  /**
   * Creates an execution request using the context's flow and correlation identifiers.
   *
   * <p>This convenience method constructs an {@link ExecutionRequest} by combining the context's
   * workflow identifiers with the provided metadata and payload. This is typically used when a task
   * needs to trigger sub-executions or communicate with other services.
   *
   * <p>The created request inherits the {@code flowId} and {@code correlationId} from this context,
   * ensuring proper tracing and workflow continuity.
   *
   * @param metadata additional metadata to include in the execution request. This may contain
   *     configuration parameters, headers, or other contextual information needed for the
   *     execution. Must not be {@code null}.
   * @param payload the actual data payload to be processed during execution. This contains the
   *     business data that the task will operate on. Must not be {@code null}.
   * @return a new {@link ExecutionRequest} configured with this context's identifiers and the
   *     provided metadata and payload
   * @throws NullPointerException if metadata or payload is {@code null}
   * @see ExecutionRequest
   */
  public ExecutionRequest createRequest(
      Map<String, Serializable> metadata, Map<String, Serializable> payload) {
    return new ExecutionRequest(flowId, correlationId, metadata, payload);
  }
}
