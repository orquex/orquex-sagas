package co.orquex.sagas.domain.flow;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

/**
 * The flow configuration contains the flow timeout and the all-or-nothing flag required to handle
 * the flow behaviour.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "timeout": "PT1M",
 *   "allOrNothing": false
 * }
 * </pre>
 *
 * @param timeout the flow timeout, by default, is 1 minute.
 * @param allOrNothing the all-or-nothing flag
 */
public record FlowConfiguration(Duration timeout, boolean allOrNothing) implements Serializable {

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
