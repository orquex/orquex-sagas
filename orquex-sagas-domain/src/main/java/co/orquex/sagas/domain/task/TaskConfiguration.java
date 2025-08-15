package co.orquex.sagas.domain.task;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.task.resilience.TaskResilienceConfiguration;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;

/**
 * This class is used to define and manage the configuration and behavior of a task.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "executor": "executor-name",
 *   "resilience": {},
 *   "parameters": {}
 * }
 * </pre>
 *
 * @see TaskResilienceConfiguration
 */
@Builder
public record TaskConfiguration(
        String executor, TaskResilienceConfiguration resilience, Map<String, Serializable> parameters)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  public static final String DEFAULT_EXECUTOR = "default";

  public TaskConfiguration {
    executor = checkArgumentNotNullOrElse(executor, DEFAULT_EXECUTOR);
    parameters = checkArgumentNotNullOrElse(parameters, new HashMap<>());
  }
}
