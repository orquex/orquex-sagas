package co.orquex.sagas.task.http.client;

import co.orquex.sagas.task.http.api.HttpClientProvider;
import java.util.Base64;
import java.util.List;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.message.BasicHeader;

public class BasicHttpClientProvider implements HttpClientProvider<HttpClient> {

  @Override
  public HttpClient getClient() {
    final var authorizationValue =
        "Basic " + Base64.getEncoder().encodeToString("name:password".getBytes());
    final var authorization = new BasicHeader("Authorization", authorizationValue);
    return HttpClientBuilder.create().setDefaultHeaders(List.of(authorization)).build();
  }

  @Override
  public String getKey() {
    return "basic-client";
  }
}
