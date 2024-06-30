package co.orquex.sagas.domain.transaction;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * The Transaction encapsulates the status of a flow, including its unique identifiers, data, and
 * timestamps.
 */
@Getter
@Builder
public class Transaction implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  private String transactionId;
  private String flowId;
  private String correlationId;
  private Serializable data;
  private Status status;
  private Instant startedAt;
  private Instant updatedAt;
  private Instant expiresAt;
}
