package co.orquex.sagas.domain.stage;

import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotEmpty;
import static co.orquex.sagas.domain.utils.Preconditions.checkArgumentNotNullOrElse;

import co.orquex.sagas.domain.version.OrquexSagasVersion;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.*;

@Getter
public final class Activity extends Stage {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;
  private final List<ActivityTask> activityTasks;
  private final boolean parallel;
  private final String outgoing;
  private final boolean allOrNothing;

  public Activity(
      final String id,
      final String name,
      final Map<String, Serializable> metadata,
      final StageConfiguration configuration,
      final List<ActivityTask> activityTasks,
      final boolean parallel,
      final String outgoing,
      final Boolean allOrNothing) {
    super(StageType.activity.name(), id, name, metadata, configuration);
    this.activityTasks =
        checkArgumentNotEmpty(
            activityTasks, "activity '%s' does not contains tasks".formatted(super.getName()));
    this.outgoing = outgoing; // null if is the last stage
    this.parallel = parallel;
    this.allOrNothing = checkArgumentNotNullOrElse(allOrNothing, true);
  }
}
