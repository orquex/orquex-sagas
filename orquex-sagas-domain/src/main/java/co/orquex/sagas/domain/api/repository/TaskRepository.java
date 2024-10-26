package co.orquex.sagas.domain.api.repository;

import co.orquex.sagas.domain.task.Task;
import java.util.Optional;

/** Repository for managing tasks. */
public interface TaskRepository {

  /**
   * Find a task by its ID.
   *
   * @param id the task ID.
   * @return the task if found, empty otherwise.
   */
  Optional<Task> findById(String id);
}
