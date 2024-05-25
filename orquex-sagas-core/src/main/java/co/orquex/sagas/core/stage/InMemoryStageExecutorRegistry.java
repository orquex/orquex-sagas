package co.orquex.sagas.core.stage;

import co.orquex.sagas.domain.api.StageExecutor;
import co.orquex.sagas.domain.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStageExecutorRegistry implements Registry<StageExecutor> {

  private final Map<String, StageExecutor> registry;

  private InMemoryStageExecutorRegistry() {
    this.registry = new ConcurrentHashMap<>();
  }

  public static InMemoryStageExecutorRegistry of(final List<StageExecutor> stageExecutorList) {
    final var instance = new InMemoryStageExecutorRegistry();
    stageExecutorList.forEach(instance::add);
    return instance;
  }

  public static InMemoryStageExecutorRegistry newInstance() {
    return new InMemoryStageExecutorRegistry();
  }

  @Override
  public void add(StageExecutor stageExecutor) {
    this.registry.put(stageExecutor.getId(), stageExecutor);
  }

  @Override
  public Optional<StageExecutor> get(String id) {
    // Cast to the specific type before returning
    return Optional.ofNullable(this.registry.get(id));
  }
}
