package co.orquex.sagas.task.http.api;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestHttpPostTask extends AbstractHttpClientTaskImplementation<TestHttpClient> {

  public TestHttpPostTask(
      HttpClientProviderRegistry<TestHttpClient> registry, ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  @Override
  protected HttpActivityResponse doRequest(
      TestHttpClient client, HttpActivityRequest activityRequest) {
    final var response =
        client
            .post()
            .url(activityRequest.url())
            .headers(activityRequest.headers())
            .body(activityRequest.payload())
            .execute();

    return new HttpActivityResponse(response.code(), response.body(), response.headers());
  }

  @Override
  public String getKey() {
    return "http-post-test";
  }
}
