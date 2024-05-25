package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Builder;

@Builder
public record StageConfiguration(String implementation, boolean aon, Map<String, Serializable> parameters) implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  public static final String DEFAULT_IMPLEMENTATION = "default";
  public static final boolean DEFAULT_ALL_OR_NOTHING = false;

  public StageConfiguration {
    implementation = checkArgumentNotNullOrElse(implementation, DEFAULT_IMPLEMENTATION);
    parameters = checkArgumentNotNullOrElse(parameters, new HashMap<>());
  }

  public StageConfiguration() {
    this(DEFAULT_IMPLEMENTATION, DEFAULT_ALL_OR_NOTHING, null);
  }
}
