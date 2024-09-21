package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.domain.api.registry.Registry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/** HTTP POST operation using OkHttp client. */
@Slf4j
public class OkHttpPostActivity extends OkHttpAbstractTaskImplementation {

  public OkHttpPostActivity(Registry<OkHttpClientProvider> registry) {
    super(registry);
  }

  @Override
  protected Request doRequest(OkHttpActivityRequest activityRequest) {
    return new Request.Builder()
        .url(activityRequest.url())
        .post(
            RequestBody.create(
                convertValue(activityRequest.body()), MediaType.get("application/json")))
        .headers(getHeaders(activityRequest.headers()))
        .build();
  }

  @Override
  public String getKey() {
    return "okhttp-post";
  }
}
