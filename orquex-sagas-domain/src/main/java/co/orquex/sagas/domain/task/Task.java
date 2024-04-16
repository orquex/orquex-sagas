package co.orquex.sagas.domain.task;

import static co.orquex.sagas.domain.utils.Preconditions.*;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public record Task(
    String id,
    String name,
    String implementation,
    Processor compensation,
    Map<String, Serializable> metadata,
    TaskConfiguration configuration)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public Task {
    implementation = checkArgumentNotEmpty(implementation, "task`s implementation required");
    metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
    configuration = checkArgumentNotNullOrElse(configuration, TaskConfiguration.builder().build());
  }
}
