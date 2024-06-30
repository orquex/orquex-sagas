package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.*;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * The Evaluation is a specialized type of Stage that contains logic to determine the next stage to
 * execute based on certain conditions. It includes an EvaluationTask that encapsulates the task for
 * evaluating the conditions, a list of Condition objects that define the conditions to be
 * evaluated, and a defaultOutgoing string that specifies the ID of the default stage to transition
 * to if none of the conditions are met.
 *
 * <p>JSON representation:
 *
 * <pre>
 * {
 *   "id": "stage-id",
 *   "type": "evaluation",
 *   "name": "stage-name",
 *   "metadata": {},
 *   "configuration": {},
 *   "evaluationTask": {},
 *   "conditions": [],
 *   "defaultOutgoing": "stage-id"
 * }
 * </pre>
 *
 * @see Stage
 */
@Getter
public final class Evaluation extends Stage {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  private final EvaluationTask evaluationTask;
  private final List<Condition> conditions;
  private final String defaultOutgoing;

  public Evaluation(
      final String id,
      final String name,
      final Map<String, Serializable> metadata,
      final StageConfiguration configuration,
      final EvaluationTask evaluationTask,
      final List<Condition> conditions,
      final String defaultOutgoing) {
    super(StageType.evaluation.name(), id, name, metadata, configuration);
    this.evaluationTask =
        checkArgumentNotNull(
            evaluationTask,
            "evaluation '%s' requires an evaluation task".formatted(super.getName()));
    this.conditions = checkArgumentNotEmpty(conditions, "evaluation's conditions required");
    this.defaultOutgoing =
        checkArgumentNotEmpty(defaultOutgoing, "evaluation's default outgoing required");
  }
}
