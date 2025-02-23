package co.orquex.sagas.domain.stage;

import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;

/**
 * This class is used to initiate the execution of a specific stage in a workflow.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "transactionId": "transaction-id",
 *   "stage": {},
 *   "executionRequest": {}
 * }
 * </pre>
 *
 * @see Stage
 * @see ExecutionRequest
 */
@Builder
public record StageRequest(String transactionId, Stage stage, ExecutionRequest executionRequest)
    implements Serializable {
  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
}
