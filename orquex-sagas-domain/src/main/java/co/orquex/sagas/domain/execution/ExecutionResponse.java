package co.orquex.sagas.domain.execution;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents an execution response in a synchronous workflow.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "transactionId": "",
 *   "payload": {}
 * }
 * </pre>
 */
public record ExecutionResponse(String transactionId, Map<String, Serializable> payload)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
}
