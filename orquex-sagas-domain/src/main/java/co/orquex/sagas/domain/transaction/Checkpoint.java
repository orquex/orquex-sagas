package co.orquex.sagas.domain.transaction;

import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;

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
