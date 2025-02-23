package co.orquex.sagas.task.okhttp;

import co.orquex.sagas.task.http.api.HttpClientProvider;
import okhttp3.*;

public class BasicOkHttpClientProvider implements HttpClientProvider<OkHttpClient> {

  @Override
  public OkHttpClient getClient() {
    return new OkHttpClient.Builder()
        .addInterceptor(
            chain -> {
              final var credential = Credentials.basic("name", "password");
              final var request =
                  chain.request().newBuilder().header("Authorization", credential).build();
              return chain.proceed(request);
            })
        .build();
  }

  @Override
  public String getKey() {
    return "basic-client";
  }
}
