package co.orquex.sagas.task.http.client;

import co.orquex.sagas.task.http.api.HttpActivityRequest;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

/** HTTP PATCH operation using Apache HTTP client. */
public class HttpClientPatchActivity extends HttpClientAbstractTaskImplementation {

  public HttpClientPatchActivity(HttpClientProviderRegistry<HttpClient> registry,
          ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  /**
   * Creates the HTTP PATCH request with headers and body if provided.
   *
   * @param activityRequest the activity request containing URL, headers, and body
   * @return the configured HttpPatch request
   */
  @Override
  protected HttpUriRequestBase doRequest(HttpActivityRequest activityRequest) {
    final var httpPatch = new HttpPatch(activityRequest.url());
    httpPatch.setHeaders(getHeaders(activityRequest.headers()));
    final var requestBody = activityRequest.body();
    if (requestBody != null && !requestBody.isEmpty()) {
      final var json = writeValueAsString(requestBody);
      httpPatch.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
    }
    return httpPatch;
  }

  /**
   * Returns the key identifier for this activity.
   *
   * @return the key "http-patch"
   */
  @Override
  public String getKey() {
    return "http-patch";
  }
}
