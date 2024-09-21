package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.domain.api.registry.Registry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

/** HTTP GET operation using OkHttp client. */
@Slf4j
public class OkHttpGetActivity extends OkHttpAbstractTaskImplementation {

  public OkHttpGetActivity(Registry<OkHttpClientProvider> registry) {
    super(registry);
  }

  @Override
  protected Request doRequest(OkHttpActivityRequest activityRequest) {
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
