package co.orquex.sagas.core.task;

import co.orquex.sagas.domain.api.TaskExecutor;
import co.orquex.sagas.domain.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTaskExecutorRegistry implements Registry<TaskExecutor> {

  private final Map<String, TaskExecutor> registry;

  private InMemoryTaskExecutorRegistry() {
    this.registry = new ConcurrentHashMap<>();
  }

  public static InMemoryTaskExecutorRegistry of(final List<TaskExecutor> taskExecutorList) {
    final var instance = new InMemoryTaskExecutorRegistry();
    taskExecutorList.forEach(instance::add);
    return instance;
  }

  public static InMemoryTaskExecutorRegistry newInstance() {
    return new InMemoryTaskExecutorRegistry();
  }

  @Override
  public void add(TaskExecutor taskExecutor) {
    this.registry.put(taskExecutor.getId(), taskExecutor);
  }

  @Override
  public Optional<TaskExecutor> get(String id) {
    // Cast to the specific type before returning
    return Optional.ofNullable(this.registry.get(id));
  }
}
