package co.orquex.sagas.domain.task;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public record Processor(String task, Map<String, Serializable> metadata) implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public Processor {
    task = checkArgumentNotEmpty(task, "processor's task ID required");
    metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
  }
}
