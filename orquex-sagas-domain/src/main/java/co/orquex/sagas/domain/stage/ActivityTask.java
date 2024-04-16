package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.task.Processor;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public record ActivityTask(
    String task,
    Processor preProcessor,
    Processor postProcessor,
    Processor compensation,
    Map<String, Serializable> metadata)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public ActivityTask {
    task = checkArgumentNotEmpty(task, "activity's task required");
    metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
  }

  public ActivityTask(String task) {
    this(task, null, null, null, null);
  }
}
