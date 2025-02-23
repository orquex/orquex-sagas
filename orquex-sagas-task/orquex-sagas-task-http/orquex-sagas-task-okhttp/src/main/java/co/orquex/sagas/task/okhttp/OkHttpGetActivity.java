package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.task.http.api.HttpActivityRequest;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/** HTTP GET operation using OkHttp client. */
@Slf4j
public class OkHttpGetActivity extends OkHttpAbstractTaskImplementation {

  public OkHttpGetActivity(
      HttpClientProviderRegistry<OkHttpClient> registry, ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  @Override
  protected Request doRequest(HttpActivityRequest activityRequest) {
    return new Request.Builder()
        .url(activityRequest.url())
        .headers(getHeaders(activityRequest.headers()))
        .build();
  }

  @Override
  public String getKey() {
    return "okhttp-get";
  }
}
