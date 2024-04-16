package co.orquex.sagas.domain.exception;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;

public class WorkflowException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

    public WorkflowException(String message) {
        super(message);
    }
}
