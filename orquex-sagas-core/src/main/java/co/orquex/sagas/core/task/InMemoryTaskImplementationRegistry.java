package co.orquex.sagas.core.task;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTaskImplementationRegistry implements Registry<TaskImplementation> {

  private final Map<String, TaskImplementation> registry;

  private InMemoryTaskImplementationRegistry() {
    this.registry = new ConcurrentHashMap<>();
  }

  public static InMemoryTaskImplementationRegistry of(final List<TaskImplementation> stagedList) {
    final var instance = new InMemoryTaskImplementationRegistry();
    stagedList.forEach(instance::add);
    return instance;
  }

  @Override
  public void add(TaskImplementation implementation) {
    this.registry.put(implementation.getKey(), implementation);
  }

  @Override
  public Optional<TaskImplementation> get(String name) {
    // Cast to the specific type before returning
    return Optional.ofNullable(this.registry.get(name));
  }
}
