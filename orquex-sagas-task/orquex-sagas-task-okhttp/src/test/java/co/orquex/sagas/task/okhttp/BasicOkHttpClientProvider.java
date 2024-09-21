package co.orquex.sagas.task.okhttp;

import okhttp3.*;

public class BasicOkHttpClientProvider implements OkHttpClientProvider {

  @Override
  public OkHttpClient getOkHttpClient() {
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
