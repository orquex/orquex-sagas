package co.orquex.sagas.task.http.client;

import co.orquex.sagas.task.http.api.HttpClientProvider;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.hc.client5.http.classic.HttpClient;

/** In-memory registry for {@link HttpClientProvider} of {@link HttpClient}. */
public class HttpClientInMemoryClientProviderRegistry
    implements HttpClientProviderRegistry<HttpClient> {

  private final Map<String, HttpClientProvider<HttpClient>> registry;

  private HttpClientInMemoryClientProviderRegistry() {
    this.registry = new ConcurrentHashMap<>();
  }

  public static HttpClientInMemoryClientProviderRegistry of(
      final List<HttpClientProvider<HttpClient>> providerList) {
    final var instance = new HttpClientInMemoryClientProviderRegistry();
    providerList.forEach(instance::add);
    return instance;
  }

  /**
   * Adds a provider to the registry.
   *
   * @param provider the HTTP client provider to add
   */
  @Override
  public void add(HttpClientProvider<HttpClient> provider) {
    this.registry.put(provider.getKey(), provider);
  }

  /**
   * Retrieves a provider by name.
   *
   * @param name the name of the provider
   * @return an Optional containing the provider if found
   */
  @Override
  public Optional<HttpClientProvider<HttpClient>> get(String name) {
    // Cast to the specific type before returning
    return Optional.ofNullable(this.registry.get(name));
  }
}
