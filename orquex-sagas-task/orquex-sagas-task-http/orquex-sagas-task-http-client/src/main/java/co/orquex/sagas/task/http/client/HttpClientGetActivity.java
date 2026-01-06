package co.orquex.sagas.task.http.client;

import co.orquex.sagas.task.http.api.HttpActivityRequest;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/** HTTP GET operation using Apache HTTP client. */
public class HttpClientGetActivity extends HttpClientAbstractTaskImplementation {

  public HttpClientGetActivity(HttpClientProviderRegistry<HttpClient> registry,
          ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  /**
   * Creates the HTTP GET request with headers.
   *
   * @param activityRequest the activity request containing URL and headers
   * @return the configured HttpGet request
   */
  @Override
  protected HttpUriRequestBase doRequest(HttpActivityRequest activityRequest) {
    final var httpGet = new HttpGet(activityRequest.url());
    httpGet.setHeaders(getHeaders(activityRequest.headers()));
    return httpGet;
  }

  /**
   * Returns the key identifier for this activity.
   *
   * @return the key "http-get"
   */
  @Override
  public String getKey() {
    return "http-get";
  }
}
