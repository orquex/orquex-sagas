package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.task.http.api.HttpActivityRequest;
import co.orquex.sagas.task.http.api.HttpClientProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/** HTTP DELETE operation using OkHttp client. */
@Slf4j
public class OkHttpDeleteActivity extends OkHttpAbstractTaskImplementation {

  public OkHttpDeleteActivity(
      HttpClientProviderRegistry<OkHttpClient> registry, ObjectMapper objectMapper) {
    super(registry, objectMapper);
  }

  @Override
  protected Request doRequest(HttpActivityRequest activityRequest) {
    return new Request.Builder()
        .url(activityRequest.url())
        .delete()
        .headers(getHeaders(activityRequest.headers()))
        .build();
  }

  @Override
  public String getKey() {
    return "okhttp-delete";
  }
}
