package co.orquex.sagas.domain.transaction;

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
 *   "transactionId": "transaction-id",
 *   "task": "task-name",
 *   "metadata": {},
 *   "request": {},
 *   "response": {},
 *   "createdAt": "timestamp"
 * }
 * </pre>
 *
 * @see co.orquex.sagas.domain.stage.ActivityTask
 * @see co.orquex.sagas.domain.task.TaskProcessor
 */
public record Compensation(
    String transactionId,
    String task,
    Map<String, Serializable> metadata,
    Map<String, Serializable> request,
    Map<String, Serializable> response,
    Instant createdAt)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public Compensation {
    createdAt = Instant.now();
  }
}
