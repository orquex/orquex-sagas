package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNull;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.*;

/**
 * The stage is the main unit of work in the flow, every stage is unique and contains all the
 * information required to execute it.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "id": "stage-id",
 *   "type": "stage-type",
 *   "name": "stage-name",
 *   "metadata": {},
 *   "configuration": {},
 * }
 * </pre>
 *
 * @see Activity
 * @see Evaluation
 */
@Getter
public sealed class Stage implements Serializable permits Activity, Evaluation {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  private final String type;
  private final String id;
  private final String name;
  @With private Map<String, Serializable> metadata;
  private final StageConfiguration configuration;

  public Stage(
      final String type,
      final String id,
      final String name,
      final Map<String, Serializable> metadata,
      final StageConfiguration configuration) {
    this.type = checkArgumentNotNull(type, "stage's type required");
    this.id = checkArgumentNotNullOrElse(id, UUID.randomUUID().toString());
    this.name = checkArgumentNotNullOrElse(name, this.id);
    this.metadata = checkArgumentNotNullOrElse(metadata, new HashMap<>());
    this.configuration = checkArgumentNotNullOrElse(configuration, new StageConfiguration());
  }
}
