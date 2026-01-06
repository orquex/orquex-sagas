package co.orquex.sagas.domain.exception;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;

/**
 * Exception thrown when an error occurs during workflow execution in the Orquex Sagas framework.
 *
 * <p>This runtime exception is used to indicate various types of workflow-related failures, such
 * as:
 *
 * <ul>
 *   <li>Workflow definition validation errors
 *   <li>Stage execution failures
 *   <li>Task execution errors
 *   <li>Workflow state transition issues
 *   <li>Configuration or setup problems
 * </ul>
 *
 * <p>As a {@link RuntimeException}, this exception does not need to be explicitly declared in
 * method signatures, allowing for cleaner workflow execution code while still providing meaningful
 * error information when workflow operations fail.
 */
public class WorkflowException extends RuntimeException {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public WorkflowException(String message) {
    super(message);
  }
}
