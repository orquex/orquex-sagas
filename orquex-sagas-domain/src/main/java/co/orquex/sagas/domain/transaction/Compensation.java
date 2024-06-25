package co.orquex.sagas.domain.transaction;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

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
