package co.orquex.sagas.domain.flow;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
