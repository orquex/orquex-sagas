package co.orquex.sagas.domain.api.repository;

import co.orquex.sagas.domain.task.Task;
import java.util.Optional;

public interface TaskRepository {

  Optional<Task> findById(String id);
}
