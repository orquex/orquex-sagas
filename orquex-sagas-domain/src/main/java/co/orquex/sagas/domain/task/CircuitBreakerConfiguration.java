package co.orquex.sagas.domain.task;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;

public record CircuitBreakerConfiguration(long maxPerPeriod) implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
}
