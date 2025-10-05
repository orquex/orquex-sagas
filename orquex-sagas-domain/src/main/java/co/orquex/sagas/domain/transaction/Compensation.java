package co.orquex.sagas.domain.transaction;

import co.orquex.sagas.domain.task.TaskProcessor;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Represents a compensation event message in a workflow transaction.
 *
 * <p>A Compensation is generated every time an activity task is executed, and it contains a
 * compensation task processor. It encapsulates the transaction identifier, task name, metadata,
 * request and response data, and the timestamp when it was created.
 *
 * <p>This class is sent via an event for further processing.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "id": "compensation-id",
 *   "transactionId": "transaction-id",
 *   "flowId": "flow-id",
 *   "correlationId": "correlation-id",
 *   "task": "task-name",
 *   "metadata": {},
 *   "request": {},
 *   "response": {},
 *   "status": "status",
 *   "createdAt": "timestamp"
 * }
 * </pre>
 *
 * @see co.orquex.sagas.domain.stage.ActivityTask
 * @see co.orquex.sagas.domain.task.TaskProcessor
 */
public record Compensation(
    String id,
    String transactionId,
    String flowId,
    String correlationId,
    String task,
    Map<String, Serializable> metadata,
    Map<String, Serializable> request,
    Map<String, Serializable> response,
    TaskProcessor preProcessor,
    TaskProcessor postProcessor,
    Status status,
    Instant createdAt,
    Instant updatedAt)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  // Method to create a new Compensation instance with an updated status
  public Compensation withStatus(Status status) {
    return new Compensation(
        this.id,
        this.transactionId,
        this.flowId,
        this.correlationId,
        this.task,
        this.metadata,
        this.request,
        this.response,
        this.preProcessor,
        this.postProcessor,
        status,
        this.createdAt,
        Instant.now());
  }
}
