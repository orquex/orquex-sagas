package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.task.http.api.HttpClientProvider;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.OkHttpClient;

/** In-memory registry for {@link HttpClientProvider} of {@link OkHttpClient}. */
public class OkHttpInMemoryClientProviderRegistry
    implements HttpClientProviderRegistry<OkHttpClient> {

  private final Map<String, HttpClientProvider<OkHttpClient>> registry;

  private OkHttpInMemoryClientProviderRegistry() {
    this.registry = new ConcurrentHashMap<>();
  }

  public static OkHttpInMemoryClientProviderRegistry of(
      final List<HttpClientProvider<OkHttpClient>> providerList) {
    final var instance = new OkHttpInMemoryClientProviderRegistry();
    providerList.forEach(instance::add);
    return instance;
  }

  @Override
  public void add(HttpClientProvider<OkHttpClient> provider) {
    this.registry.put(provider.getKey(), provider);
  }

  @Override
  public Optional<HttpClientProvider<OkHttpClient>> get(String name) {
    // Cast to the specific type before returning
    return Optional.ofNullable(this.registry.get(name));
  }
}
