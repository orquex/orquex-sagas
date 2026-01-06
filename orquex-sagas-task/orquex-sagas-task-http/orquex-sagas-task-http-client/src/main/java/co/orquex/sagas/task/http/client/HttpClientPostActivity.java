package co.orquex.sagas.task.http.client;

import co.orquex.sagas.task.http.api.HttpActivityRequest;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

/** HTTP POST operation using Apache HTTP client. */
public class HttpClientPostActivity extends HttpClientAbstractTaskImplementation {

  public HttpClientPostActivity(HttpClientProviderRegistry<HttpClient> registry,
          ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  /**
   * Creates the HTTP POST request with headers and body if provided.
   *
   * @param activityRequest the activity request containing URL, headers, and body
   * @return the configured HttpPost request
   */
  @Override
  protected HttpUriRequestBase doRequest(HttpActivityRequest activityRequest) {
    final var httpPost = new HttpPost(activityRequest.url());
    httpPost.setHeaders(getHeaders(activityRequest.headers()));
    final var requestBody = activityRequest.body();
    if (requestBody != null && !requestBody.isEmpty()) {
      final var json = writeValueAsString(requestBody);
      httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
    }
    return httpPost;
  }

  /**
   * Returns the key identifier for this activity.
   *
   * @return the key "http-post"
   */
  @Override
  public String getKey() {
    return "http-post";
  }
}
