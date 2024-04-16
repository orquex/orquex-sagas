package co.orquex.sagas.domain.event;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;

@Builder
public record Error(String transactionId,
                    String flowId,
                    String correlationId,
                    String status,
                    String message,
                    String detail,
                    Instant timestamp)
    implements Serializable {

  public Error {
    status = checkArgumentNotNullOrElse(status, message);
    timestamp = checkArgumentNotNullOrElse(timestamp, Instant.now());
  }

}
