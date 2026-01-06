package co.orquex.sagas.task.http.client;

import co.orquex.sagas.task.http.api.HttpActivityRequest;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/** HTTP DELETE operation using Apache HTTP client. */
public class HttpClientDeleteActivity extends HttpClientAbstractTaskImplementation {

  public HttpClientDeleteActivity(HttpClientProviderRegistry<HttpClient> registry,
          ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  /**
   * Creates the HTTP DELETE request with headers.
   *
   * @param activityRequest the activity request containing URL and headers
   * @return the configured HttpDelete request
   */
  @Override
  protected HttpUriRequestBase doRequest(HttpActivityRequest activityRequest) {
    final var httpDelete = new HttpDelete(activityRequest.url());
    httpDelete.setHeaders(getHeaders(activityRequest.headers()));
    return httpDelete;
  }

  /**
   * Returns the key identifier for this activity.
   *
   * @return the key "http-delete"
   */
  @Override
  public String getKey() {
    return "http-delete";
  }
}
