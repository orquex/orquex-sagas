package co.orquex.sagas.sample.cs.service.repository;

import co.orquex.sagas.domain.api.repository.TaskRepository;
import co.orquex.sagas.domain.task.Task;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;

@Repository
public class InMemoryTaskRepository implements TaskRepository {

  private final ConcurrentMap<String, Task> taskMap;

  public InMemoryTaskRepository(ObjectMapper objectMapper) throws IOException {
    final var file = ResourceUtils.getFile("classpath:data/tasks.json");
    final var tasks = objectMapper.readValue(file, new TypeReference<List<Task>>() {});
    this.taskMap = tasks.stream().collect(Collectors.toConcurrentMap(Task::id, task -> task));
  }

  @Override
  public Optional<Task> findById(String id) {
    return Optional.ofNullable(taskMap.get(id));
  }
}
