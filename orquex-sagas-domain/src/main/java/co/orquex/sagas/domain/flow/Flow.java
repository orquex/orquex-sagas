package co.orquex.sagas.domain.flow;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The flow is the sagas' definition, every flow is unique and contains all the stages required to
 * execute it.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "id": "flow-id",
 *   "name": "flow-name",
 *   "initialStage": "stage-id",
 *   "stages": {},
 *   "metadata": {},
 *   "configuration": {}
 * }
 * </pre>
 *
 * @param id a required unique identifier for the flow
 * @param name a human-readable name for the flow
 * @param initialStage the required initial stage to start the flow
 * @param stages a required map containing all the stages required to execute the flow
 * @param metadata a map containing additional information about the flow
 * @param configuration the flow's configuration
 */
public record Flow(
    String id,
    String name,
    String initialStage,
    Map<String, Stage> stages,
    Map<String, Serializable> metadata,
    FlowConfiguration configuration)
    implements Serializable {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public Flow {
    id = checkArgumentNotEmpty(id, "flow's id required");
    name = checkArgumentNotNullOrElse(name, id); // if name not present set the id
    stages = checkArgumentNotEmpty(stages, "flow's stages required");
    initialStage = checkArgumentNotEmpty(initialStage, "flow's initial stage required");
    // Check if the initial stage already exists in the stage's map
    if (!stages.containsKey(initialStage))
      throw new IllegalArgumentException(
          "flow '%s' does not contains the initial stage '%s'".formatted(name, initialStage));
    metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
    configuration = checkArgumentNotNullOrElse(configuration, new FlowConfiguration());
  }

  public Flow(String id, String initialStage, Map<String, Stage> stages) {
    this(id, null, initialStage, stages, null, null);
  }
}
