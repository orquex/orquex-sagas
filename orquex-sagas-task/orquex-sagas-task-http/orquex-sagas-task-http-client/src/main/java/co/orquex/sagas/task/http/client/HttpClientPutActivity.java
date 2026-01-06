package co.orquex.sagas.task.http.client;

import co.orquex.sagas.task.http.api.HttpActivityRequest;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

/** HTTP PUT operation using Apache HTTP client. */
public class HttpClientPutActivity extends HttpClientAbstractTaskImplementation {

  protected HttpClientPutActivity(
      HttpClientProviderRegistry<HttpClient> registry, ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  /**
   * Creates the HTTP PUT request with headers and body if provided.
   *
   * @param activityRequest the activity request containing URL, headers, and body
   * @return the configured HttpPut request
   */
  @Override
  protected HttpUriRequestBase doRequest(HttpActivityRequest activityRequest) {
    final var httpPut = new HttpPut(activityRequest.url());
    httpPut.setHeaders(getHeaders(activityRequest.headers()));
    final var requestBody = activityRequest.body();
    if (requestBody != null && !requestBody.isEmpty()) {
      final var json = writeValueAsString(requestBody);
      httpPut.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
    }
    return httpPut;
  }

  /**
   * Returns the key identifier for this activity.
   *
   * @return the key "http-put"
   */
  @Override
  public String getKey() {
    return "http-put";
  }
}
