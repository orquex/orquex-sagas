package co.orquex.sagas.task.http.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestHttpClientProviderRegistry implements HttpClientProviderRegistry<TestHttpClient> {

  private static final TestHttpClientProviderRegistry INSTANCE =
      new TestHttpClientProviderRegistry();
  private final Map<String, HttpClientProvider<TestHttpClient>> registry = new HashMap<>();

  private TestHttpClientProviderRegistry() {}

  public static TestHttpClientProviderRegistry getInstance() {
    return INSTANCE;
  }

  @Override
  public void add(HttpClientProvider<TestHttpClient> type) {
    registry.put(type.getKey(), type);
  }

  @Override
  public Optional<HttpClientProvider<TestHttpClient>> get(String id) {
    return Optional.ofNullable(registry.get(id));
  }
}
