package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;

/**
 * The Condition is evaluated during the execution of an {@link EvaluationTask}; it contains an
 * expression that defines the outgoing string that specifies the ID of the stage to transition to
 * if the condition is met.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "expression": "condition-expression",
 *   "outgoing": "stage-id"
 * }
 * </pre>
 *
 * @see Evaluation
 * @see EvaluationTask
 */
public record Condition(String expression, String outgoing) implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public Condition {
    expression = checkArgumentNotEmpty(expression, "condition's expression required");
    outgoing = checkArgumentNotEmpty(outgoing, "condition's outgoing required");
  }
}
