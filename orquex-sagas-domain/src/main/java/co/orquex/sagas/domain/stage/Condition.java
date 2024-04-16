package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;

public record Condition(String expression, String outgoing) implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public Condition {
    expression = checkArgumentNotEmpty(expression, "condition's expression required");
    outgoing = checkArgumentNotEmpty(outgoing, "condition's outgoing required");
  }
}
