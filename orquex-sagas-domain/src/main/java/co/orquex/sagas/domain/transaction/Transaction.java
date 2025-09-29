package co.orquex.sagas.domain.transaction;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * The Transaction encapsulates the status of a flow, including its unique identifiers, data, and
 * timestamps.
 */
public record Transaction(
    String transactionId,
    String flowId,
    String correlationId,
    Serializable data,
    Status status,
    Instant startedAt,
    Instant updatedAt,
    Instant expiresAt)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public Transaction withStatus(Status status) {
    return new Transaction(
        this.transactionId,
        this.flowId,
        this.correlationId,
        this.data,
        status,
        this.startedAt,
        Instant.now(),
        this.expiresAt);
  }
}
