package co.orquex.sagas.domain.flow;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

public record FlowConfiguration(Duration timeout, boolean aon) implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);
  public static final boolean DEFAULT_ALL_OR_NOTHING = false;

  public FlowConfiguration {
    timeout = checkArgumentNotNullOrElse(timeout, DEFAULT_TIMEOUT);
  }

  public FlowConfiguration() {
    this(DEFAULT_TIMEOUT, DEFAULT_ALL_OR_NOTHING);
  }
}
