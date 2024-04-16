package co.orquex.sagas.domain.execution;

import static co.orquex.sagas.domain.utils.Maps.merge;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.With;

/**
 *
 *
 * <pre>
 * {
 *     "flowId": "",
 *     "correlationId": "",
 *     "metadata": {},
 *     "payload": {}
 * }
 * </pre>
 */
@With
public record ExecutionRequest(
    String flowId,
    String correlationId,
    Map<String, Serializable> metadata,
    Map<String, Serializable> payload)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public ExecutionRequest {
    flowId = checkArgumentNotEmpty(flowId, "execution request's flow id is required");
    correlationId =
        checkArgumentNotEmpty(correlationId, "execution request's correlation id is required");
    metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
    payload = checkArgumentNotNullOrElse(payload, new HashMap<>());
  }

  public ExecutionRequest(String flowId, String correlationId) {
    this(flowId, correlationId, null, null);
  }

  /**
   * Merge and overwrites the current metadata with a new one.
   *
   * @param metadata a new metadata instance
   * @return a new instance of the ExecutionRequest
   */
  public ExecutionRequest mergeMetadata(final Map<String, Serializable> metadata) {
    return this.withMetadata(merge(this.metadata, metadata));
  }

  /**
   * Merge and overwrites the current payload with a new one.
   *
   * @param payload a new payload instance
   * @return a new instance of the ExecutionRequest
   */
  public ExecutionRequest mergePayload(final Map<String, Serializable> payload) {
    return this.withPayload(merge(this.payload, payload));
  }
}
