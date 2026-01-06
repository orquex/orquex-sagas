package co.orquex.sagas.sample.config;

import co.orquex.sagas.domain.jackson.OrquexJacksonModule;
import co.orquex.sagas.task.http.api.HttpClientProvider;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BasicSampleConfig {

  /** Add jackson support for the sagas to the current object mapper */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder -> builder.modulesToInstall(new OrquexJacksonModule());
  }

  @Bean
  public HttpClientProvider<HttpClient> httpClientBasicProvider() {
    return new HttpClientProvider<>() {
      @Override
      public HttpClient getClient() {
        final var connectionConfig =
            ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(30)).build();
        final var connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build();
        final var requestConfig =
            RequestConfig.custom().setResponseTimeout(Timeout.ofSeconds(30)).build();

        return HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();
      }

      @Override
      public String getKey() {
        return "basic-provider";
      }
    };
  }
}
