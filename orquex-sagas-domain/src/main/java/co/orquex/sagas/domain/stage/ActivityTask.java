package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.task.TaskProcessor;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The activity task contains the information necessary to pre-process the input request of a task,
 * then execute it and finally post-process its response, while recording its compensation.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
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
    String task,
    TaskProcessor preProcessor,
    TaskProcessor postProcessor,
    TaskProcessor compensation,
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
