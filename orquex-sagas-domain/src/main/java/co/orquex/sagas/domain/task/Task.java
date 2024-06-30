package co.orquex.sagas.domain.task;

import static co.orquex.sagas.domain.utils.Preconditions.*;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to define and manage the details of a specific task in a workflow.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "id": "task-id",
 *   "name": "task-name",
 *   "implementation": "task-implementation",
 *   "compensation": {},
 *   "metadata": {},
 *   "configuration": {}
 * }
 * </pre>
 *
 * @see TaskProcessor
 * @see TaskConfiguration
 */
public record Task(
    String id,
    String name,
    String implementation,
    TaskProcessor compensation,
    Map<String, Serializable> metadata,
    TaskConfiguration configuration)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public Task {
    implementation = checkArgumentNotEmpty(implementation, "Task`s implementation required");
    metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
    configuration = checkArgumentNotNullOrElse(configuration, TaskConfiguration.builder().build());
  }
}
