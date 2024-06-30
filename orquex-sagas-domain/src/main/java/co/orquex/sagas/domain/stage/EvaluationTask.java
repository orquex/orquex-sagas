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
 * Represents an evaluation task within an Evaluation stage in a workflow.
 *
 * <p>An EvaluationTask is a description of the task that will be executed during the evaluation
 * process. It contains the ID of the task, an optional pre-processing task processor, and metadata
 * that can be used to change the settings or behavior of the task execution.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "task": "task-id",
 *   "preProcessor": {},
 *   "metadata": {}
 * }
 * </pre>
 *
 * @see Evaluation
 */
public record EvaluationTask(
    String task, TaskProcessor preProcessor, Map<String, Serializable> metadata)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public EvaluationTask {
    task = checkArgumentNotEmpty(task, "evaluation's task required");
    metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
  }

  public EvaluationTask(String taskId) {
    this(taskId, null, null);
  }
}
