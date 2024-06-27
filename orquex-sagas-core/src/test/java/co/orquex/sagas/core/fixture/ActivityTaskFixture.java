package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.stage.ActivityTask;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActivityTaskFixture {

  public static ActivityTask getSimpleActivityTask(String id) {
    return new ActivityTask(id, null, null, null, null);
  }
}
