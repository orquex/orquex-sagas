package co.orquex.sagas.domain.event;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import lombok.Builder;

@Builder
public record Error(
    String transactionId,
    String flowId,
    String correlationId,
    String status,
    String message,
    String detail,
    Instant timestamp)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public Error {
    status = checkArgumentNotNullOrElse(status, message);
    timestamp = checkArgumentNotNullOrElse(timestamp, Instant.now());
  }
}
