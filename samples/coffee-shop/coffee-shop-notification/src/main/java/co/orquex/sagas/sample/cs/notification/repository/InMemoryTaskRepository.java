package co.orquex.sagas.sample.cs.notification.repository;

import co.orquex.sagas.domain.repository.TaskRepository;
import co.orquex.sagas.domain.task.Task;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryTaskRepository implements TaskRepository {

  private final ConcurrentMap<String, Task> tasks;

  public InMemoryTaskRepository(ObjectMapper objectMapper) throws IOException {
    final var file = ResourceUtils.getFile("classpath:data/tasks.json");
    final var tasks = objectMapper.readValue(file, new TypeReference<List<Task>>() {});
    this.tasks = tasks.stream().collect(Collectors.toConcurrentMap(Task::id, task -> task));
  }

  @Override
  public Optional<Task> findById(String id) {
    return Optional.ofNullable(tasks.get(id));
  }
}
