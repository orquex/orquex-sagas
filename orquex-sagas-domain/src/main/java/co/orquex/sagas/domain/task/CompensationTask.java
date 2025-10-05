package co.orquex.sagas.domain.task;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.stage.ActivityTask;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The compensation task contains the information necessary to pre-process the input request of a
 * task, then execute it and finally post-process its response.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "task": "task-id",
 *   "name": "compensation-name",
 *   "preProcessor": {},
 *   "postProcessor": {},
 *   "metadata": {}
 * }
 * </pre>
 *
 * @see ActivityTask
 */
public record CompensationTask(
    String task,
    String name,
    TaskProcessor preProcessor,
    TaskProcessor postProcessor,
    Map<String, Serializable> metadata)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public CompensationTask {
    task = checkArgumentNotEmpty(task, "compensation's task required");
    name = checkArgumentNotNullOrElse(name, task);
    metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
  }
}
