package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.task.TaskProcessor;
import java.io.Serializable;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskProcessorFixture {

  public static TaskProcessor getTaskProcessor(String id) {
    return getTaskProcessor(id, null);
  }

  public static TaskProcessor getTaskProcessor(String id, Map<String, Serializable> metadata) {
    return new TaskProcessor(id, metadata);
  }
}
