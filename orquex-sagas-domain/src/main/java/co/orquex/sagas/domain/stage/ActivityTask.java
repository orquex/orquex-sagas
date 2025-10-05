package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.task.CompensationTask;
import co.orquex.sagas.domain.task.TaskProcessor;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The activity task contains the information necessary to pre-process the input request of a task,
 * then execute it and finally post-process its response, while recording its compensation.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "id": "",
 *   "name": "Task name"
 *   "task": "task-name",
 *   "preProcessor": {},
 *   "postProcessor": {},
 *   "compensation": {},
 *   "metadata": {}
 * }
 * </pre>
 *
 * @see Activity
 */
public record ActivityTask(
    String id,
    String name,
    String task,
    TaskProcessor preProcessor,
    TaskProcessor postProcessor,
    CompensationTask compensation,
    Map<String, Serializable> metadata)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public ActivityTask {
    id = checkArgumentNotNullOrElse(id, UUID.randomUUID().toString());
    name = checkArgumentNotNullOrElse(name, id);
    task = checkArgumentNotEmpty(task, "activity's task required");
    metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
  }

  public ActivityTask(String task) {
    this(UUID.randomUUID().toString(), task, task, null, null, null, null);
  }
}
