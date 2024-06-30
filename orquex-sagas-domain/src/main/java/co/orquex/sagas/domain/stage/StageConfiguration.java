package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;

/**
 * The stage configuration contains the implementation and parameters required to execute the stage.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "implementation": "implementation-name",
 *   "parameters": {}
 * }
 * </pre>
 *
 * @see Stage
 */
@Builder
public record StageConfiguration(String implementation, Map<String, Serializable> parameters) implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  public static final String DEFAULT_IMPLEMENTATION = "default";

  public StageConfiguration {
    implementation = checkArgumentNotNullOrElse(implementation, DEFAULT_IMPLEMENTATION);
    parameters = checkArgumentNotNullOrElse(parameters, new HashMap<>());
  }

  public StageConfiguration() {
    this(DEFAULT_IMPLEMENTATION, null);
  }
}
