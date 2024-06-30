package co.orquex.sagas.domain.transaction;

import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;

/**
 * Represents a checkpoint in a workflow transaction.
 *
 * <p>A Checkpoint is created every time a stage is executed in a workflow. It encapsulates the
 * status of the execution, including the transaction and flow identifiers, correlation identifier,
 * metadata, request and response data, and timestamps. It also includes the outgoing stage
 * identifier and the incoming stage.
 *
 * <p>Each execution of a stage will generate multiple checkpoints with different statuses, allowing
 * for tracking and auditing of the workflow.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "status": "status",
 *   "transactionId": "transaction-id",
 *   "flowId": "flow-id",
 *   "correlationId": "correlation-id",
 *   "metadata": {},
 *   "request": {},
 *   "response": {},
 *   "outgoing": "outgoing-stage-id",
 *   "incoming": {},
 *   "createdAt": "timestamp",
 *   "updatedAt": "timestamp"
 * }
 * </pre>
 *
 * @see Status
 * @see Stage
 */
@Builder
public record Checkpoint(
    Status status,
    String transactionId,
    String flowId,
    String correlationId,
    Map<String, Serializable> metadata,
    Map<String, Serializable> request,
    Map<String, Serializable> response,
    String outgoing,
    Stage incoming,
    Instant createdAt,
    Instant updatedAt)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
}
