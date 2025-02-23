package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;

/**
 * This class is used to capture the response from the execution of a specific stage in a workflow.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "transactionId": "transaction-id",
 *   "payload": {},
 *   "outgoing": "outgoing-stage-id"
 * }
 * </pre>
 *
 * @see Stage
 * @see StageRequest
 */
@Builder
public record StageResponse(
    String transactionId, Map<String, Serializable> payload, String outgoing)
    implements Serializable {

  public StageResponse {
    payload = checkArgumentNotNullOrElse(payload, new HashMap<>());
  }

  public StageResponse(String transactionId) {
    this(transactionId, null, null);
  }
}
