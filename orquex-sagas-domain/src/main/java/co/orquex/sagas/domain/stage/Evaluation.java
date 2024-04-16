package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.*;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public final class Evaluation extends Stage {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  public static final String EXPRESSION = "__expression";
  public static final String RESULT = "__result";
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
    this.evaluationTask = checkArgumentNotNull(evaluationTask, "evaluation '%s' requires an evaluation task"
            .formatted(super.getName()));
    this.conditions = checkArgumentNotEmpty(conditions, "evaluation's conditions required");
    this.defaultOutgoing = checkArgumentNotEmpty(defaultOutgoing, "evaluation's default outgoing required");
  }
}
