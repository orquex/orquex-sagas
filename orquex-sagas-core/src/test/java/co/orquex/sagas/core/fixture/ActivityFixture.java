package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.ActivityTask;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActivityFixture {

  public static Activity getSimpleActivity(
      String id, List<ActivityTask> activityTasks, boolean parallel, boolean allOrNothing) {
    return new Activity(id, null, null, null, activityTasks, parallel, null, allOrNothing);
  }
}
