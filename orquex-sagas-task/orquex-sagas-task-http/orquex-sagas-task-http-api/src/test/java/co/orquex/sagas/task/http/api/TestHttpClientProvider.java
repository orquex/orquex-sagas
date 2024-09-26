package co.orquex.sagas.task.http.api;

import co.orquex.sagas.task.http.api.TestHttpClient.TestHttpResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TestHttpClientProvider implements HttpClientProvider<TestHttpClient> {

  public static final String HTTP_CLIENT_PROVIDER_TEST = "http-client-provider-test";
  private final TestHttpResponse expectedResponse;

  @Override
  public TestHttpClient getClient() {
    return new TestHttpClient(expectedResponse);
  }

  @Override
  public String getKey() {
    return HTTP_CLIENT_PROVIDER_TEST;
  }
}
