package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.domain.api.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory registry for {@link OkHttpClientProvider}. */
public class OkHttpInMemoryClientRegistry implements Registry<OkHttpClientProvider> {

  private final Map<String, OkHttpClientProvider> registry;

  private OkHttpInMemoryClientRegistry() {
    this.registry = new ConcurrentHashMap<>();
  }

  public static OkHttpInMemoryClientRegistry of(final List<OkHttpClientProvider> stagedList) {
    final var instance = new OkHttpInMemoryClientRegistry();
    stagedList.forEach(instance::add);
    return instance;
  }

  @Override
  public void add(OkHttpClientProvider provider) {
    this.registry.put(provider.getKey(), provider);
  }

  @Override
  public Optional<OkHttpClientProvider> get(String name) {
    // Cast to the specific type before returning
    return Optional.ofNullable(this.registry.get(name));
  }
}
